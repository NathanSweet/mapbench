# mapbench

This repository contains add, contains, and remove benchmarks for these hash sets:

## Cuckoo
This is a [cuckoo](https://wikipedia.org/wiki/Cuckoo_hashing) hash set used in [libgdx](https://libgdx.badlogicgames.com/) and [Kryo](https://github.com/EsotericSoftware/kryo/) for many years. It has some advantages, notably constant worst lookup time. It has a major disadvantage though: if > ~50 keys have the same hash codes, it fails because the backing array is doubled until it runs out of memory.

## java.util.HashSet
This is convenient since it comes with the JRE. It uses [chaining](https://wikipedia.org/wiki/Hash_table#Separate_chaining) and generally has good properties, except that it allocates when items are added. Allocation makes it not a good fit for games or other applications that need to avoid GC.

## Merry
This is a [linear probing](https://wikipedia.org/wiki/Linear_probing) hash set that uses the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci hashing, instead of the more common power of two mask, to better distribute poor hashCodes (see [Malte Skarupke's blog post](https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/)). Linear probing continues to work even when all hashcodes collide, just more slowly (O(n) worst case).

# Results

![](http://n4te.com/x/7775-add.png)

![](http://n4te.com/x/7776-contains.png)

![](http://n4te.com/x/7777-remove.png)

Data: http://n4te.com/x/7778-results.zip
