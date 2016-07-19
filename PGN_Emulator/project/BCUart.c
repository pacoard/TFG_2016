/* --COPYRIGHT--,BSD
 * Copyright (c) 2012, Texas Instruments Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * *  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * *  Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * --/COPYRIGHT--*/
 
#include "msp430.h"
#include "BCUart.h"
#include "timer_b.h"
#include "PGN_protocol.h"

// Receive buffer for the UART.  Incoming bytes need a place to go immediately,
// otherwise there might be an overrun when the next comes in.  The USCI ISR
// puts them here.
uint8_t  bcUartRcvBuf[BC_RXBUF_SIZE];

// The index within bcUartRcvBuf, where the next byte will be written.
uint16_t bcUartRcvBufIndex = 0;

// Boolean flag indicating whether bcUartRcvBufIndex has reached the
// threshold BC_RX_WAKE_THRESH.  0 = FALSE, 1 = TRUE
uint8_t  bcUartRxThreshReached = 0;

uint8_t  rxUart = FALSE;
uint8_t  uartTimeoutExpired = FALSE;
uint16_t counterTimer;


// Initializes the USCI_A1 module as a UART, using baudrate settings in
// bcUart.h.  The baudrate is dependent on SMCLK speed.
void bcUartInit(void)
{
    // Always use the step-by-step init procedure listed in the USCI chapter of
    // the F5xx Family User's Guide
    UCA1CTL1 |= UCSWRST;        // Put the USCI state machine in reset
    UCA1CTL1 |= UCSSEL__SMCLK;  // Use SMCLK as the bit clock

    // Set the baudrate
    UCA1BR0 = UCA1_BR0;
    UCA1BR1 = UCA1_BR1;
    UCA1MCTL = (UCA1_BRF << 4) | (UCA1_BRS << 1) | (UCA1_OS);

    P4SEL |= BIT4+BIT5;         // Configure these pins as TXD/RXD

    UCA1CTL1 &= ~UCSWRST;       // Take the USCI out of reset
    UCA1IE |= UCRXIE;           // Enable the RX interrupt.  Now, when bytes are
                                // rcv'ed, the USCI_A1 vector will be generated.
}

// Inicializa USCI_A1 para el launchpad con 9600 bps
void bcUartInitLaunchpad(void)
{
    UCA1CTL1 |= UCSWRST;        // Put the USCI state machine in reset
    UCA1CTL1 |= UCSSEL_1;       // CLK = ACLK

    // Set the baudrate
    UCA1BR0 = 0x03;                           // 32kHz/9600=3.41 (see User's Guide)
    UCA1BR1 = 0x00;                           //
    UCA1MCTL = UCBRS_3+UCBRF_0;               // Modulation UCBRSx=3, UCBRFx=0

    P4SEL |= BIT4+BIT5;         // Configure these pins as TXD/RXD

    UCA1CTL1 &= ~UCSWRST;       // Take the USCI out of reset
    UCA1IE |= UCRXIE;           // Enable the RX interrupt.  Now, when bytes are
                                // rcv'ed, the USCI_A1 vector will be generated.
}

// Inicializa USCI_A0 para el PGN con la configuración de lucanu (9600 bps)
void bcUartInitPGN(void)
{
    UCA0CTL1 |= UCSWRST;        // Put the USCI state machine in reset
    UCA0CTL1 |= UCSSEL_1;       // CLK = ACLK

    // Set the baudrate
    UCA0BR0 = 0x03;                           // 32kHz/9600=3.41 (see User's Guide)
    UCA0BR1 = 0x00;                           //
    UCA0MCTL = UCBRS_3+UCBRF_0;               // Modulation UCBRSx=3, UCBRFx=0

    P3SEL |= BIT3+BIT4;         // Configure these pins as TXD/RXD

    UCA0CTL1 &= ~UCSWRST;       // Take the USCI out of reset
    UCA0IE |= UCRXIE;           // Enable the RX interrupt.  Now, when bytes are
                                // rcv'ed, the USCI_A1 vector will be generated.
}


// Sends 'len' bytes, starting at 'buf'
void bcUartSend(uint8_t * buf, uint8_t len)
{
    uint8_t i = 0;

    // Write each byte in buf to USCI TX buffer, which sends it out
    while (i < len)
    {
        UCA1TXBUF = *(buf+(i++));

        // Wait until each bit has been clocked out...
        while(!(UCTXIFG==(UCTXIFG & UCA1IFG))&&((UCA1STAT & UCBUSY)==UCBUSY));
    }
}


// Copies into 'buf' whatever bytes have been received on the UART since the
// last fetch.  Returns the number of bytes copied.
uint16_t bcUartReceiveBytesInBuffer(uint8_t* buf)
{
    uint16_t i, count;

    // Hold off ints for incoming data during the copy
    UCA1IE &= ~UCRXIE;

    for(i=0; i<bcUartRcvBufIndex; i++)
    {
        buf[i] = bcUartRcvBuf[i];
    }

    count = bcUartRcvBufIndex;
    bcUartRcvBufIndex = 0;     // Move index back to the beginning of the buffer
    bcUartRxThreshReached = 0;

    // Restore USCI interrupts, to resume receiving data.
    UCA1IE |= UCRXIE;

    return count;
}

// Reads a command from the UART (keeps reading until EOF) and stores it in buf
uint16_t bcUartReadCommand(uint8_t* buf)
{
    uint16_t i, count;
    uint16_t charPair = 0x0000;

    while (charPair != EOF)
    {
      // Hold off ints for incoming data during the copy
      UCA1IE &= ~UCRXIE;
      
      for(i=0; i<bcUartRcvBufIndex; i++)
      {
          buf[i] = bcUartRcvBuf[i];
      }
  
      count = bcUartRcvBufIndex;
      charPair = (buf[bcUartRcvBufIndex-2] << 8) | buf[bcUartRcvBufIndex-1];  // Last two bytes
  
      // Restore USCI interrupts, to resume receiving data.
      UCA1IE |= UCRXIE;
    }
    
    bcUartRcvBufIndex = 0;     // Move index back to the beginning of the buffer
    bcUartRxThreshReached = 0;
    return count;
}

// Reads a command from the UART (keeps reading until EOF or
// the timeout expires) and stores it in buf
uint16_t bcUartReadCommandTimeout(uint8_t* buf)
{
    uint16_t i, count;
    uint16_t charPair = 0x0000;
    uartTimeoutExpired = FALSE;

    while ((charPair != EOF)&&(!uartTimeoutExpired))
    {
      rxUart = FALSE;
      uartTimeoutExpired = FALSE;
      
      // Start the timer
      counterTimer = 0;
      timerInit(50000);
           
      // Wait for uart or timeout
      while ((!rxUart)&&(!uartTimeoutExpired));
      
      if(!uartTimeoutExpired) {
        // Hold off ints for incoming data during the copy
        UCA1IE &= ~UCRXIE;
        for(i=0; i<bcUartRcvBufIndex; i++)
        {
            buf[i] = bcUartRcvBuf[i];
        }
        count = bcUartRcvBufIndex;
        charPair = (buf[bcUartRcvBufIndex-2] << 8) | buf[bcUartRcvBufIndex-1];  // Last two bytes
      
        // Restore USCI interrupts, to resume receiving data.
        UCA1IE |= UCRXIE;
      }  
      
      else
      {
        count = -1;
      }
    }
    
    bcUartRcvBufIndex = 0;     // Move index back to the beginning of the buffer
    bcUartRxThreshReached = 0;
    return count;
}

void timerInit(uint16_t rate)
{
  TIMER_B_startUpMode(   TIMER_B0_BASE,
                            TIMER_B_CLOCKSOURCE_SMCLK,
                            TIMER_B_CLOCKSOURCE_DIVIDER_1,
                            rate,
                            TIMER_B_TBIE_INTERRUPT_DISABLE,
                            TIMER_B_CAPTURECOMPARE_INTERRUPT_ENABLE,
                            TIMER_B_DO_CLEAR
                            );
}
  
// The USCI_A1 receive interrupt service routine (ISR).  Executes every time a
// byte is received on the back-channel UART.
#pragma vector=USCI_A1_VECTOR
__interrupt void bcUartISR(void)
{
    bcUartRcvBuf[bcUartRcvBufIndex++] = UCA1RXBUF;  // Fetch the byte, store
                                                    // it in the buffer.
    rxUart = TRUE;

    // Wake main, to fetch data from the buffer.
    if(bcUartRcvBufIndex >= BC_RX_WAKE_THRESH)
    {
        bcUartRxThreshReached = 1;
        __bic_SR_register_on_exit(LPM3_bits);       // Exit LPM0-3
    }
}

// This is the Timer B0 interrupt vector service routine.
#pragma vector=TIMERB0_VECTOR
__interrupt void TIMERB0_ISR(void)
{
   counterTimer++;
   if (counterTimer == TIMEOUT_UART) {
      uartTimeoutExpired = TRUE;
      counterTimer = 0;
   }
}
