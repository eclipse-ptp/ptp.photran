.PHONY: all clean
SRC=$(wildcard *.f90)

GFORTRAN=`which gfortran 2>/dev/null | head -1`
IFORT=`which ifort 2>/dev/null | head -1`
DEBUGSWITCH=-g
OPTSWITCH=-O4
OUTSWITCH=-o

all:
	@if [ "$(GFORTRAN)" != "" ]; then \
	    for file in $(SRC); do \
	        $(GFORTRAN) -fopenmp \
	                    $(DEBUGSWITCH) \
	                    $(OUTSWITCH) `echo $$file | sed -e 's/.f90/.exe/'` \
	                    $(OPTSWITCH) \
	                    $$file; \
	    done; \
	elif [ "$(IFORT)" != "" ]; then \
	    for file in $(SRC); do \
	        $(IFORT)    -openmp \
	                    $(DEBUGSWITCH) \
	                    $(OUTSWITCH) `echo $$file | sed -e 's/.f90/.exe/'` \
	                    $(OPTSWITCH) \
	                    $$file; \
	    done; \
	fi

clean:
	rm -f *.exe *.mod