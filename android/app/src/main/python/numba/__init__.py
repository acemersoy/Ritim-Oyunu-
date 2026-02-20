"""
Mock numba module for Android/Chaquopy.
Provides no-op decorators so librosa imports work without actual JIT compilation.
"""

import os
os.environ["NUMBA_DISABLE_JIT"] = "1"


def jit(*args, **kwargs):
    """No-op jit decorator."""
    if len(args) == 1 and callable(args[0]):
        return args[0]
    def wrapper(func):
        return func
    return wrapper


def njit(*args, **kwargs):
    """No-op njit decorator."""
    return jit(*args, **kwargs)


def vectorize(*args, **kwargs):
    """No-op vectorize decorator."""
    return jit(*args, **kwargs)


def guvectorize(*args, **kwargs):
    """No-op guvectorize decorator."""
    return jit(*args, **kwargs)


def stencil(*args, **kwargs):
    """No-op stencil decorator."""
    return jit(*args, **kwargs)


def cfunc(*args, **kwargs):
    """No-op cfunc decorator."""
    return jit(*args, **kwargs)


def generated_jit(*args, **kwargs):
    """No-op generated_jit decorator."""
    return jit(*args, **kwargs)


def prange(*args, **kwargs):
    """Fallback to range."""
    return range(*args)


# Types that numba provides
class types:
    int32 = int
    int64 = int
    float32 = float
    float64 = float
    boolean = bool
    void = None
    Array = None
    complex64 = complex
    complex128 = complex

    @staticmethod
    def UniTuple(dtype, count):
        return None


class config:
    DISABLE_JIT = True
