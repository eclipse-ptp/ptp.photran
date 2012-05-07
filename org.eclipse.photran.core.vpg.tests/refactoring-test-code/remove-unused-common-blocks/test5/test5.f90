program test5!<<<<< 1,1,pass
    implicit none

    integer a,b,c,d,e,f

    common a,b
    common /x/ c,d /y/ e
    common /z/ f

    a=1
    e=1

contains

subroutine subroutine1

 integer a,b,c,d

 common /s1/ a /s2/ b /s3/ c

end subroutine subroutine1

end program test5
