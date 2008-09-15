# Simple ARM assembler example.
# Outputs printable ASCII characters from ' ' to '~'.

  # int i = ' ';
  mov r0,32
  # do {
loop:
  #   putchar(i);
  swi 0
  #   ++i;
  add r0,r0,1
  # } while (i != 126);
  cmp r0,126
  bne loop
  # putchar('\n');
  mov r0,0xa
  swi 0
  bl exit123
  mov pc,lr

  mul r0,r1,r2
  mla r0,r1,r2,r3
  
  ldr r0,[r2,r0]
  ldreq r2,[r3,r4]
  strne r3,[r4,r5]
  
exit0:
  mov r2,0
  swi 0x11
exit123:
  mov r2,123
  swi 0x11
