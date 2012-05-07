program test6!<<<<< 8,5,13,11,fail-initial
    implicit none
    integer, parameter :: N=100000
    real v(N),w(N)
	integer i
	integer j

	do i=1,N
        v(i)=v(i)+1
        if (j>2) then
            print *,"this can't be refactored "
        end if
	end do
end program test6
