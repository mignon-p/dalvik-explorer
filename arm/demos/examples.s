# A nonsense "program" made up of every instruction, in order, from this page:
# http://www.peter-cockerell.net/aalp/html/ch-3.html
main:
  add r0,r1,r2
  addne r0,r0,r2
  adds r0,r0,r2
  addnes r0,r0,r2
  add r0,r0,1
#  add r0,r0,0x1000
  adcs r0,r0,r0
  add r0,r0,r0 lsl 1
  ands r0,r1,r2 lsr 1
#  ands r0,r0,r5        # Mask wanted bits using R5
  and r0,r0,0xdf        # Convert character to upper case
#  bics r0,r0,r5        # Zero unwanted bits using R5
  bic r0,r0,0x20        # Convert to caps by clearing bit 5
#  tst r0,r5            # Test bits using r5, setting flags
  tst r0,0x20           # Test case of character in R0
#  orrs r0,r0,r5        # Set desired bits using R5
#  orr r0,r0,0x80000000 # Set top bit of R0
#  eors r0,r0,r5        # Invert bits using R5, setting flags
  eor r0,r0,1           # 'Toggle' state of bit 0
#  teq r0,r5             # Test bits using R5, setting flags
  teq r0,0              # See if R0 = 0.
  mov r0,r0 lsl 2       # Multiply R0 by four.
#  movs r0,r1            # Put R1 in R0, setting flags
  mov r1,0x80           # Set R1 to 128
#  mvns r0,r0            # Invert all bits of R0, setting flags
  mvn r0,0              # Set R0 to &FFFFFFFF, i.e. -1
  
  add r0,r0,1           # Increment R0
  add r0,r0,r0 lsl 2    # Multiple R0 by 5
#  adds r2,r2,r7         # Add result; check for overflow
  
  # Add the 64-bit number in R2,R3 to that in R0,R1
#  adds r0,r0,r2         # Add the lower words, getting carry
#  adc r1,r1,r3          # Add upper words, using carry
  
  sub r0,r0,1           # Decrement R0
  sub r0,r0,r0 asr 2    # Multiply R0 by 3/4 (R0=R0-R0/4)
  
  # Subtract the 64-bit number in R2,R3 from that in R0,R1
#  subs r0,r0,r2         # Sub the lower words, getting borrow
#  sbc r1,r1,r3          # Sub upper words, using borrow

  # R0 = 1 - R1
#  mvn r0,r1             # get NOT (R1) = -R1-1
  add r0,r0,2           # get -R1-1+2 = 1-R1
  # R0 = 1 - R1
  rsb r0,r1,1           # R0 = 1 - R1
  
  # Multiply R0 by 7
#  rsb r0,r0,r0 asl 3    # Get 8*R0-R0 = 7*R0
  
  # Obtain &100000000-R0,R1 in R0,R1
  rsbs r0,r0,0          # Sub the lower words, getting borrow
  rsc r1,r1,1           # Sub the upper words
  
  cmp r0,0x100          # Check R0 takes a single byte
  
#  cmp r0,r1             # Get greater of R1, R0 in R0
  movlt r0,r1
  
  mvn r1,0              # Get -1 in R1
#  cmp r0,r1             # Do the comparison
  
  cmn r0,1              # Compare R0 with -1
  
  cmn r0,256            # Make sure R0 >= -256
  mvnlt r0,255
  
  add r0,r15,128
#  mov r0,r15 ror 26

  add r15,r15,r0
#  orrs r15,r15,0x20000000

#  teqp r15,0x20000000
  mov r0,r15
  mul r0,r1,r2


#  str r0,[r1,20]
#  str r0,[r1,-200]
#  str r0,[r1,r2 lsl 2]
#  str r0,[r1,-r2 lsl 3]
#  str r0,[r1,-16]!
  sub r1,r1,16
#  strb r0,[r1,1]!
#  strb r0,[r1],r2
#  str r2,[r4],-16

#  ldr r0,default
default:
#  str r0,label
label:

#  str r1,[r0],4
#  str r2,[r0],4
#  str r3,[r0],4
#  str r4,[r0],4
#  str r5,[r0],4
#  str r6,[r0],4
#  str r7,[r0],4
#  str r8,[r0],4
#  str r9,[r0],4
#  str r10,[r0],4
#  str r11,[r0],4
#  str r12,[r0],4

#  stmed r13!,{r1,r2,r5}
#  stmfd r13,{r0-r15}
  
#  stmia r0!,{r1,r2}
#  ldmda r0!,{r1,r3,r4}

  mov r0,0         # Init the count to zero
LOOP:
  movs r1,r1 lsr 1 # Get next 1 bit in Carry
  adc r0,r0,0      # Add it to count
  bne LOOP         # Loop if more to do
  
  movs r15,r14

#  stmfd r13!,{r0-r12} # Save user's registers
#  bic r14,r14,0xFC000003 # Mask status bits
#  ldr r13,[r14,-4] # Load SWI instruction
