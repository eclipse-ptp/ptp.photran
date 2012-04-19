  interface
    subroutine cfunction(n, x) bind(c)  ! Defined in c.c
      use iso_c_binding
      integer(kind=c_int), value :: n
      real(kind=c_float), value :: x
    end subroutine
  end interface

  print *, 'This is the Fortran program; I am going to call the C function now...'
  call cfunction(1, 2.3)
end

