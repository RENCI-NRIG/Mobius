# Table of contents

 - [Description](#descr)
 - [Installation](#install)
   - [Mobius Client](#mbclient)
     - [Usage](#usage1)
   - [Condor Client](#condorclient)
     - [Usage](#usage2)   
   - [Json Data](#json)
   - [Certificates](#certs)
   - [Mobius Client Examples](#mobius_client_examples)
     - [Create a workflow](#createworkflow)
     - [Create a compute node in a workflow](#createcompute)
     - [Create a stitch port in a workflow](#createstitchport)
     - [Get status of a workflow](#getworkflow)
     - [Delete a workflow](#deleteworkflow)
   - [Condor Client Examples](#condor_client_examples)
     - [Create a condor cluster](#create)
     - [Get status of condor cluster](#get)
     - [Delete condor cluster](#delete)
 
# <a name="descr"></a>Description
Python based clients
 - Mobius client to trigger Moobius REST commands.
 - Python client to create Condor clusters by invoking various supported Mobius REST commands.

# <a name="install"></a>Installation
`https://github.com/RENCI-NRIG/Mobius.git`

`cd Mobius/python/`

You are now ready execute python client.

Pre-requisites: requires python 3 or above version and python requests package installed

## <a name="mbclient"></a>Mobius Client
Mobius client to trigger Moobius REST commands.
