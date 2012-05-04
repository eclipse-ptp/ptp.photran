!!
!! Liebmann's method to compute heat transfer across a 2-D surface
!! J. Overbey 8/27/08, updated for OpenACC 5/3/12
!!
!! Use liebmann-viz.sh in the main project directory to visualize the resulting
!! table using gnuplot
!!
!! This version is based on the code in src-liebmann-3-loops, but
!! (1) the "iterate" subroutine is inlined,
!! (2) the number of iterations is always a multiple of 2 (delta is
!!     only computed on even-numbered iterations), and
!! (3) with an OpenACC-compliant compiler (e.g., Cray Fortran 8.0), the
!!     stencil computations will be moved to an accelerator device/GPU
!!
program liebmann_example
    implicit none

    integer, parameter :: SIZE = 4096
    integer, parameter :: INTERIOR_SIZE = SIZE - 2
    integer, parameter :: OUTPUT_SIZE = 128
    real, parameter    :: BOUNDARY_VALUE = 5.0
    real, parameter    :: EPSILON = 0.001

    call main()

contains

subroutine main()
    real :: surface(SIZE, SIZE)
    integer :: i, j

    integer :: start_time, end_time, count_rate

    call system_clock(start_time, count_rate)
    call liebmann(surface)
    call system_clock(end_time)

    do i = 1, SIZE, SIZE/OUTPUT_SIZE
        do j = 1, SIZE-1, SIZE/OUTPUT_SIZE
            write (*,'(F4.2" ")',advance="no") surface(i, j)
        end do
        write (*,'(F4.2" ")') surface(i, SIZE)
    end do

    print *, "Elapsed Time (seconds):", real(end_time - start_time) / real(count_rate)
end subroutine

subroutine liebmann(surface)
    real, dimension(SIZE, SIZE), intent(out) :: surface
    real, dimension(SIZE, SIZE) :: prev, next
    real :: delta
    integer :: i, j
    logical :: done

    call init_with_boundaries(prev)
    call init_with_boundaries(next)

    !$acc data copyin(prev,next) copyout(prev)
    done = .false.
    do while (.not. done)
        !$acc parallel loop
        do j = 2, SIZE-1
            do i = 2, SIZE-1
                next(i,j) = &
                    (prev(i-1, j) + &
                     prev(i+1, j) + &
                     prev(i, j-1) + &
                     prev(i, j+1)) / 4.0
            end do
        end do
        !$acc end parallel loop
        delta = 0.0
        !$acc parallel loop reduction(max:delta)
        do j = 2, SIZE-1
            do i = 2, SIZE-1
                prev(i,j) = &
                    (next(i-1, j) + &
                     next(i+1, j) + &
                     next(i, j-1) + &
                     next(i, j+1)) / 4.0
                 delta = max(delta, abs(prev(i,j)-next(i,j)))
            end do
        end do
        !$acc end parallel loop
        if (delta < EPSILON) then
            done = .true.
        end if
    end do
    !$acc end data
    surface = prev
end subroutine

subroutine init_with_boundaries(surface)
    real, dimension(SIZE, SIZE), intent(out) :: surface

    surface          = 0.0
    surface(1, :)    = BOUNDARY_VALUE
    surface(SIZE, :) = BOUNDARY_VALUE
    surface(:, 1)    = BOUNDARY_VALUE
    surface(:, SIZE) = BOUNDARY_VALUE
end subroutine

end program
