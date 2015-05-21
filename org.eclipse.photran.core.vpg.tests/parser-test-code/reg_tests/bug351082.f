      program bug351082
          implicit none
          real, allocatable :: j(:,:,:)
          integer real_alloced

          allocate(J(-10:10,-10:10,-10:10))
          real_alloced =real_alloced+size(J)
      end program bug351082
