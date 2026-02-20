import numpy as np


def centroids_to_lanes(centroids):
    if not centroids:
        return []

    arr = np.array(centroids)
    p20, p40, p60, p80 = np.percentile(arr, [20, 40, 60, 80])

    lanes = []
    for c in centroids:
        if c <= p20:
            lanes.append(1)
        elif c <= p40:
            lanes.append(2)
        elif c <= p60:
            lanes.append(3)
        elif c <= p80:
            lanes.append(4)
        else:
            lanes.append(5)
    return lanes
