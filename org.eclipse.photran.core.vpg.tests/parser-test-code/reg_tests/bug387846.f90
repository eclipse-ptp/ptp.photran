program main
    implicit none

    INTEGER :: array(5) = (/1:5/)
    INTEGER :: enumerator

    ! Non, standard, so I care less about this one than the others below.
    !    INTERFACE
    !       INTEGER FUNCTION aFunction [C,ALIAS:'aFunction'] ()
    !       END FUNCTION
    !    END INTERFACE

    ! Regular enumertors are fine, but adding the :: and assiging a value doesnt work
    ENUM, BIND(C)
        ENUMERATOR :: a = 1
    END ENUM

    enumerator = 5

    ! It likes ELSEWHERE but not ELSE WHERE (or ENDWHERE, I think)
    WHERE (array .eq. 2 )
        array = 10
    ELSEWHERE(array > 4)
        array = 10
    ELSE WHERE(array < 2)
        array = 10
    END WHERE

! JO -- Nonstandard
!    ! This is OK
!    array = [1:5]
!
!    ! This isn't
!    CALL a_sub([1:5])

CONTAINS

! JO -- Nonstandard
!    ! It expects functions with no arguments to have an empty ()
!    INTEGER FUNCTION return_seven
!        return_seven = 7
!    END FUNCTION

    SUBROUTINE a_sub(an_array)
        INTEGER, INTENT(IN) :: an_array(:)
    END SUBROUTINE

end program main
