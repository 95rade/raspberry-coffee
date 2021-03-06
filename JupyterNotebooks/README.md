## [Jupyter Notebooks](https://jupyter.org/), for Java
Emergence of AI and Deep Learning contributed to the popularity of the Jupyter Notebooks.

Notebooks contain **_executable_** code (<u>yes</u>, you can execute the code of a Notebook in real time), possibly (extensively) commented and illustrated.
They can be re-played or just read or seen as they were after their last execution.

As `markdown` language is supported, they can contain whatever `md` syntax supports, images, url, graphics, tables, etc.
As such, graphical data representation can be used. And in the Deep Learning space, they are, indeed.

Notebooks are ideal for tutorials, hence (a part of) their success.

Notebooks are not only for Python (as they were at the very beginning), they also supports _**many**_ other languages!
The main requirement being to have a `REPL` (**R**ead **E**valuate **P**rint **L**oop).

Java 9 comes with a `REPL` (called `JShell`).

To install Java 9 on the Raspberry Pi, see [here](https://www.raspberrypi.org/forums/viewtopic.php?t=200232). 
> Note: Some restrictions may apply, the Raspberry Pi Zero might not like it.

<!-- sudo apt-get remove ca-certificates-java -->
> April 2019: Still having problems to install JDK 9 on a Raspberry Pi B3+ ... Certificate stuff.
> But all the features described here can be run on a system where Java 9 is happy.


Install Jupyter on the Raspberry Pi is easy:
```
 $ sudo pip3 install jupyter
```

To add the required Java features, see 
- <https://blog.frankel.ch/teaching-java-jupyter-notebooks/>
- <https://github.com/SpencerPark/IJava>
- <https://hub.mybinder.org/user/spencerpark-ijava-binder-ey9zwplq/notebooks/3rdPartyDependency.ipynb>


### Raspberry-Coffee Notebooks

Will provide examples as notebooks, for the features presented in this project.

From this directory (_here_, when this file you're reading is), just run
```
 $ jupyter notebook
```
And from a browser any where on the network of the Raspberry Pi, `raspberry-pi` being the name or address of the Raspberry Pi where the notebook server is running, reach `http://raspberry-pi:8888/tree` to start playing!
 
Come back here soon.

---
