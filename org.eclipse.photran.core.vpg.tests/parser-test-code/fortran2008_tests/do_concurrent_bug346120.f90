program docon
    dimension a(10)
    do i = 1, 10 ! OK
        a(i) = i
    end do
    do concurrent (i = 1:10) ! Syntax error
        a(i) = i
    end do
end program docon
