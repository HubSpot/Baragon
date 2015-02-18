## Install Baragon with Vagrant
Follow the instructions to create a pair of virtual machines that will run BaragonService and BaragonAgent

This setup is only meant for testing purposes; to give you the possibility to quickly take a look and experiment with the available tools and APIs.

The setup steps have been tested on mac computers running MAC OS X 10.10.x but they should as well work on any recent Linux distribution.

## Getting Started

- Install [Vagrant](http://www.vagrantup.com/downloads.html)

- Install [Virtualbox](https://www.virtualbox.org/wiki/Downloads)

- Install the `vagrant-hostsupdater` plugin:

```bash
vagrant plugin install vagrant-hostsupdater
```

- Clone Baragon from *github.com* in your preferred directory and go into the *vagrant* directory inside the cloned project:

```bash
git clone git@github.com:HubSpot/Baragon.git
cd Baragon/vagrant
```

Look for the provided *Vagrantfile* that contains the required vagrant commands for setting up a *VirtualBox* VM with all required software. The baragon-develop vagrant box comes with nginx, java, andzookeeper aready installed, then utilizes the shell provisioner to build BaragonService or BaragonAgent along with the neccessary configuration. There are several VMs available (described below).

- To bring up a single BaragonService and BaragonAgent, simply run:

```bash
vagrant up
```

This command will first setup and then bring up the virtual machine. The first time you run this, you should be patient because it needs to download a basic Linux image and then build Baragon. When this is done the first time, every other time that you run *vagrant up*, it will take only a few seconds to boot the virtual machine up.

During the installation your local machine hosts file is configured with the VM IPs and you will be asked to provide your password.

## Available VMs

- `service` : Runs BaragonService / zookeeper
- `agent` : Runs a BaragonAgent in the `vagrant` load balancer group
- `agent{x}` : Runs a BaragonAgent in the `vagrant{x}` load balancer group, where x can be from 2 to 5
- `base_image` : Provisioned with Java, Nginx, and Zookeeper. All other VMs start from this image

## Baragon base box

In order to speed up Vagrant provisioning, we publish a "box" to Vagrant Cloud containing Java, Nginx, and Zookeeper preinstalled. All roles depend on this box.

### Publishing a Baragon base box

To publish a new Baragon base box (i.e. for new versions of Mesos):

1. Provision the `base_image` Vagrant role: `vagrant up base_image`. This will take 10-15 minutes.
2. Verify Nginx, ZK are running
3. Export the box: `vagrant package base_image --output baragon-develop-X.Y.Z` (where X.Y.Z is the current version). This will take a few minutes.
4. Upload the newly created `baragon-develop-X.Y.Z` to a CDN. The file should be ~2 GB, so this might take awhile.
5. Go to Vagrant Cloud and publish a new box, pointing to the URL of the file you just uploaded
6. Update the `Vagrantfile` to point to this new URL.
