# Uses PMCMRplus R package for Statistical tests [![](https://jitpack.io/v/fritsche/Statistics.svg)](https://jitpack.io/#fritsche/Statistics)


* Friedman

* Kruskal


## To install dependencies

### Ubuntu and Debian

#### `PMCMRplus`

Check for the header files by running the following commands outside of R from the console.
```bash
dpkg -p libgmp-dev
dpkg -p libmpfr-dev
```
If any (or both) of the above packages are missing, simply install the missing package(s) from the repository of your Linux distribution:

```bash
sudo apt-get install libgmp-dev
sudo apt-get install libmpfr-dev
```
After successful installation of the above Linux packages, repeat with the installation of the R package PMCMRplus from within R:

```R
install.packages("PMCMRplus")
```

**If `mvtnorm` R package is missing it is possible to install using Ubuntu ppa**

```bash
# find the package
sudo apt search mvtnorm
# install it
sudo apt install r-cran-mvtnorm
```

#### `scmamp`

```bash
sudo apt-get -y build-dep libcurl4-gnutls-dev
sudo apt-get -y install libcurl4-gnutls-dev
sudo apt-get install libcurl4-openssl-dev libssl-dev
``` 

```R
if (!require("devtools")) {
  install.packages("devtools")
}

devtools::install_github("b0rxa/scmamp")
```



More info: https://github.com/b0rxa/scmamp

### For other distros

- https://cran.r-project.org/web/packages/PMCMRplus/readme/README.html
