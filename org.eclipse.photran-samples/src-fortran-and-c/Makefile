all:
	gcc -g -c c.c
	gfortran -g -c fortran.f90
	gfortran -o a.exe -g c.o fortran.o

clean:
	rm -f *.o a.exe