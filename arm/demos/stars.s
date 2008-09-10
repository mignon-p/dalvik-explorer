# Simple ARM assembler example.
# Outputs 20 '*' characters.

  mov r1,42 # character
  mov r2,20 # loop count
.loop
  mov r0,r1
  swi 0x00
  sub r2,r2,1
  cmp r2,0
  bne loop
  swi 0x03
  mov r2,0
  swi 0x11
  
