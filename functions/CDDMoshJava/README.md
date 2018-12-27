# CDD Mosh Java

## What is CDD?

Cloud Device Driver (CDD) is a framework that makes developers more efficient when writing Lambda
code for Greengrass Cores.  See the `CDDBaseline` README for more information.

## What is this function?

This is a function that lets you use Mosh to remotely access your Greengrass core via AWS IoT.  It uses
[mosh](https://mosh.org/) to make this magic happen.  Using the new, no container isolation mode it starts `mosh-server`
with the uid/gid specified in the configuration file.

**Note: If you have already done a deployment and built your config.json without the `allowFunctionsToRunAsRoot` option
you'll need to update your config.json and add that option before this function will work**

## How do I use it?

Use the AWS client's `greengrass-mosh` command.  It will give you tab completion of things in your account
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

*NOTE: If you are using fish instead of bash you'll need to prefix the command with `env`*

The terminal that you get is on the Greengrass host and will stay connected as long as the Greengrass Core and your
local system can connect to AWS IoT.  It will reconnect if your network/IP changes as well.  Reconnecting may take up
to 60 seconds so be patient if you see mosh's blue status bar and it says it can't talk to the host.

**Note: If you close the AWS client the proxy is shut down and the mosh connections will be permanently broken**
