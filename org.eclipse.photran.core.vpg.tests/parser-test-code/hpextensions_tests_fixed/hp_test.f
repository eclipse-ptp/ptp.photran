       program TEST1

        structure /top/
          character*7 abc
            structure cdate
              byte  %fill(8)        ! first method of %fill, with (x)
            end structure
          character*6 def
        end structure
       
        structure /rec_t/
          union
           map
            character*23    rec
           end map
           map
            integer*8       date
            character*15    username
           end map
           map
            character*8     cdate
            character*15    %fill   ! second method of %fill
           end map
          end union
        end structure

        record /rec_t/      user_date
       stop
       end program TEST1
