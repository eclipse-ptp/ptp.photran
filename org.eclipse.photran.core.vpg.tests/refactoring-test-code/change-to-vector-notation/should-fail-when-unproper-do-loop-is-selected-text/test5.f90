program test5!<<<<< 8,5,12,11,fail-initial
    implicit none
    integer, parameter :: N=100000
    real v(N),w(N)
	integer i
	integer j

	do i=1,N
          v(i)=v(i)+1
          w(i)=2
          j=i
	end do

end program test5
