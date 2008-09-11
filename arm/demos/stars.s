# Simple ARM assembler example.
# Outputs 20 '*' characters.

  mov r1,42 # character
  # compute a loop count of 20 (just to exercise more instructions)
  mov r2,5
  mov r2,r2 lsl 2
.loop
  mov r0,r1
  swi 0x00
  sub r2,r2,1
  cmp r2,0
  bne loop
  swi 0x03
  mov r2,0
  swi 0x11
  
