# CDD Mosh Java

## What is CDD?

Cloud Device Driver (CDD) is a framework that makes developers more efficient when writing Lambda
code for Greengrass Cores.  See the `CDDBaseline` README for more information.

## What is this function?

This is a function that lets you use Mosh to remotely access your Greengrass core via AWS IoT.  It uses [mosh](https://mosh.org/)
to make this magic happen.  Inside the Lambda container the application starts `mosh-server` which is then used as a
virtual jump host to get into the Greengrass core.

## Requirements

You must install Dropbear SSH on the host.  You must also generate a Dropbear format key and put the public portion
of the key on the host in the `.ssh/authorized_keys` directory of the user you want to connect as.

The commands I use on the host to accomplish this are:

```bash
dropbearkey -t ecdsa -f temp.ecdsa
dropbearkey -y -f temp.ecdsa >> ~/.ssh/authorized_keys
```

I then get the base64 encoding of `temp.ecdsa` so I can paste it into the Lambda container later like this:

```bash
base64 temp.ecdsa
```

## Limitations

The repository does not contain the mosh-server binary so we don't pollute it with large blobs.  You'll need
to add it to the `functions/CDDMoshJava/src/main/resources` directory.  You can get a pre-built x86_64 binary for
Ubuntu 16.04 LTS from this URL:

- [mosh-server](https://s3.amazonaws.com/timmatt-aws/shared/native-binaries/x86_64-ubuntu/mosh-server)

The `fetch-x86_64-ubuntu-binaries.sh` script in the `functions/CDDMoshJava` directory will do this for you.

## How do I use it?

Use the IoT-modular-client's `greengrass-mosh` command.  It will give you tab completion of things in your account
to make things easier.  Run `greengrass-mosh THING_NAME` where `THING_NAME` is the name of the thing associated
with the Greengrass Core.  You will see a few messages like this

```
[INFO] BasicDeviceSdkMessagingHelper: You have an existing certificate ID, checking to see if it still exists in AWS IoT
[INFO] BasicDeviceSdkMessagingHelper: Certificate still exists, checking to see if policy still exists
[INFO] BasicDeviceSdkMessagingHelper: Policy still exists
[INFO] BasicDeviceSdkMessagingHelper: You already have a certificate and private key.  We'll try to use that.  If it fails try deleting those files so we can create new ones.
Cert file:client.crt Private key: private.key
Jan 17, 2018 1:12:54 PM com.amazonaws.services.iot.client.core.AwsIotConnection onConnectionSuccess
INFO: Connection successfully established
Jan 17, 2018 1:12:54 PM com.amazonaws.services.iot.client.core.AbstractAwsIotClient onConnectionSuccess
INFO: Client connection active: modular-client
```

And if everything worked a few seconds later you'll get a message like this:

```
[INFO] StartTopic: Starting server on [64099]
[INFO] StartTopic: MOSH_KEY=QVJv8DF39g+rKRiPCVcsfA mosh-client 127.0.0.1 64099
```

Copy and paste everything on the second line after `StartTopic:` into another terminal.  mosh will start up and
connect to a local proxy that proxies your mosh traffic over AWS IoT.

When you are in the host you'll need to paste your Dropbear private key into the terminal.  Use the base64 data
you obtained in the `Requirements` section.  Run this command:

```bash
base64 -d /tmp/ecdsa.key
```

Then paste in the base64 data, press enter, then press CTRL-D to close the file.  Finally to ssh into the host
run this command:

```bash
dbclient -i /tmp/ecdsa.key USER@localhost
```

Where `USER` is the user account that has your ECDSA key in its `.ssh/authorized_keys` file.