module my_module

  type my_type
    !integer :: a
   contains
     procedure  :: write => write_mytype
  end type my_type

 contains

   subroutine write_mytype(this)
     class(my_type),intent(in)  :: this
     write (*,*) ''
   end subroutine write_mytype

end module my_module
