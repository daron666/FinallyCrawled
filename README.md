# FinallyCrawled [![Build Status](https://travis-ci.org/daron666/FinallyCrawled.svg?branch=master)](https://travis-ci.org/daron666/FinallyCrawled)
Small crawler in a pure way wow

It uses http4s as a server and sangria as a graphQL library.

To run it with Cats-Effect's IO please use `sbt "runMain org.daron.ApplicationIO"`
To run it with Monix's Task please use `sbt "runMain org.daron.ApplicationMonix"`
