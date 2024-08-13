#!/usr/bin/env python3
import os
import sys
import glob
import pandas as pd
import numpy as np
import scipy.stats as st
import matplotlib.pyplot as plt

if len(sys.argv) < 4:
    print("Usage:", sys.argv[0], "<data dir> <pattern> <lat|tp>")
    sys.exit(1)

srcdir = str(sys.argv[1])
pattern = str(sys.argv[2])
op = str(sys.argv[3])

dstsize = 0
if len(sys.argv) > 4 : dstsize = int(sys.argv[4])

if op != "lat" and op != "tp":
    print("Invalid request:", op)
    sys.exit(1)

allfiles = glob.glob(os.path.join(srcdir, pattern))
df_list = {}
df_sum = pd.DataFrame()
i = 0
for file in allfiles:
    #print "Processing ", file
    tmp = pd.read_table(file,
                        engine='python',
                        header='infer',
                        skipfooter=10,
                        usecols=["ORDER", "LATENCY", "ABS", "DSTSIZE"])

    if dstsize > 0 :
        tmp = tmp[(tmp['DSTSIZE'] == dstsize)]

    tmp['sec'] = tmp['ABS'] // 1000000
    if op == "tp":
        tmp = tmp.groupby('sec').count()
        df_sum = pd.concat((df_sum, tmp)).groupby('sec').sum()
        df_list[file] = tmp.groupby('sec').count()
    else:
        df_list[file] = tmp['LATENCY']


if op == "tp":
    grouped = df_sum
    if len(sys.argv) > 4:
        print(grouped['ORDER'])

    discarded = grouped[12:len(grouped) -
                        6]  # sub the first and last 8 seconds
    a = discarded['ORDER']
    (min, max) = st.t.interval(0.95,
                               len(a) - 1,
                               loc=np.mean(a),
                               scale=st.sem(a))
    print('#', srcdir, np.mean(a), min, max)
else:
    df = pd.concat(df_list)
    #df = df[df.VALUES != 0]  # remove lines with no latencies
    lat = df[int(len(df) * 0.1):int(len(df) * 0.9)]

    # Choose how many bins you want here
    data_set = sorted(set(lat))
    num_bins = np.append(data_set, data_set[-1] + 1)

    # Use the histogram function to bin the data
    counts, bin_edges = np.histogram(lat, bins=num_bins)  # , normed=True)
    counts = counts.astype(float) / len(lat)
    # Now find the cdf
    cdf = np.cumsum(counts)
    cdf_plot = {'value': bin_edges[0:-1], 'percentage': cdf}
    df = pd.DataFrame(cdf_plot)
    pd.set_option('display.max_rows', len(df))
    print('#', df)
    pd.reset_option('display.max_rows')
    print('#')
    print('#')
    print('#')
    print('#')
    print('#srcdir, mean, 5p, 25p, 50p, 75p, 95p, 99p')
    print('#', srcdir, lat.mean(), np.percentile(lat,
                                                 5), np.percentile(lat, 25),
          np.percentile(lat, 50), np.percentile(lat, 75),
          np.percentile(lat, 95), np.percentile(lat, 99))

    # And finally plot the cdf
    if len(sys.argv) > 5:
        plt.plot(bin_edges[0:-1], cdf)
        plt.ylim((0, 1))
        plt.show()
