! Tests to verify correct keyword/identifier resolution
program structure
    integer structure
    structure = 2
    integer fill
contains
    subroutine test
        type structure
            integer :: structure
        end type structure
        type(structure) :: t
        t%structure = 5
    end subroutine
end program structure

! Examples from the HP Fortran manual
SUBROUTINE EXAMPLE_PAGE15
    INTEGER*4 P
    STRUCTURE /ABC/
        PARAMETER (P=4)
        REAL*4 F
    END STRUCTURE
    REAL*4 A(P)
END SUBROUTINE

SUBROUTINE EXAMPLE_PAGES16_24_25
    STRUCTURE /DATE/
        INTEGER*1 DAY, MONTH
        INTEGER*2 YEAR
    END STRUCTURE
    STRUCTURE /APPOINTMENT/
        RECORD /DATE/    APP_DATE
        STRUCTURE /TIME/ APP_TIME (2)
            INTEGER*1    HOUR, MINUTE
        END STRUCTURE
        CHARACTER*20     APP_MEMO(4)
        LOGICAL*1        APP_FLAG
    END STRUCTURE

    RECORD /APPOINTMENT/ NEXT_APP,APP_LIST(10)
    PRINT *, APP_LIST(3).APP_DATE
    PRINT *, NEXT_APP.APP_MEMO(1)(1:1)

    RECORD /DATE/ TODAY, THIS_WEEK(7)
    RECORD /APPOINTMENT/ MEETING
    DO I = 1,7
        CALL GET_DATE (TODAY)
        THIS_WEEK(I) = TODAY
        THIS_WEEK(I).DAY = TODAY.DAY + 1
    END DO
    MEETING.APP_DATE = TODAY
END SUBROUTINE

SUBROUTINE EXAMPLE_PAGE18
    STRUCTURE /TEST/
        INTEGER %FILL (2,2)
        !INTEGER %FILL /1980/  ! Invalid
    END STRUCTURE
END SUBROUTINE

SUBROUTINE EXAMPLE_PAGE20
    STRUCTURE /WORDS_LONG/
        UNION
            MAP
                INTEGER*2 WORD_0, WORD_1, WORD_2
            END MAP
            MAP
                INTEGER*4 LONG
            END MAP
        END UNION
    END STRUCTURE
END SUBROUTINE
