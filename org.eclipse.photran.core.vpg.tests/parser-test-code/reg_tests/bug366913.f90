program in_bug
    implicit none
    integer, parameter :: in = selected_int_kind(1)
    integer(in) :: i

    i = 4
    print *, i, in, sub(in, i)

contains

    integer function sub(in, i)
        integer, intent(in) :: in
        integer(1), intent(in out) :: i

        i = 7
        sub = in
    end function

end program
