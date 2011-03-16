! USER STORY 6, TEST 3
! Adds SAVE attribute to the declaration statement for variables
! first_call_counter and second_call_counter since both variables are
! implicitly saved by some means

PROGRAM MyProgram !<<<<< 1, 1, pass
  CALL MySub
  CALL MySub
END PROGRAM MyProgram

SUBROUTINE MySub
  INTEGER :: first_call_counter = 0, second_call_counter
  DATA second_call_counter /10/
  first_call_counter = first_call_counter + 1
  second_call_counter = second_call_counter + 1
  PRINT *, 'called:', first_call_counter
  PRINT *, 'called:', second_call_counter
END SUBROUTINE MySub
