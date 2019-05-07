#!/bin/sh

mill resolve "spark-constraints._" | xargs -I{} mill {}.test
