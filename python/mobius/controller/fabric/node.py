#!/usr/bin/env python3
# MIT License
#
# Copyright (c) 2020 RENCI NRIG
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#
#
# Author: Komal Thareja (kthare10@renci.org)
import json
from abc import ABC, abstractmethod
from typing import Dict


class JSONField(ABC):
    @abstractmethod
    def _set_fields(self, **kwargs):
        """
        Abstract private set_fields method
        :param kwargs:
        :return:
        """

    def to_json(self) -> str:
        """
        Dumps to JSON the __dict__ of the instance. Be careful as the fields in this
        class should only be those that can be present in JSON output.
        If there are no values in the object, returns empty string.
        :return:
        """
        d = self.__dict__.copy()
        for k in self.__dict__:
            if d[k] is None or d[k] == 0:
                d.pop(k)
        if len(d) == 0:
            return ''
        return json.dumps(d, skipkeys=True, sort_keys=True)

    @classmethod
    def from_json(cls, json_string: str):
        """
        Set fields from json string and returns a new object
        :param json_string:
        :return: object
        """
        if json_string is None or len(json_string) == 0:
            return None
        d = json.loads(json_string)
        ret = cls()
        ret._set_fields(**d)
        return ret

    def to_dict(self) -> Dict[str, str] or None:
        """
        Convert to a dictionary skipping empty fields. Returns None
        if all fields are empty
        :return:
        """
        d = self.__dict__.copy()
        for k in self.__dict__:
            if d[k] is None or d[k] == 0:
                d.pop(k)
        if len(d) == 0:
            return None
        return d

    def __repr__(self):
        return self.to_json()

    def __str__(self):
        return self.to_json()

    def list_fields(self):
        l = list(self.__dict__.keys())
        l.sort()
        return l


class Node(JSONField):
    def __init__(self, **kwargs):
        self.core = 0
        self.ram = 0
        self.disk = 0
        self.image = None
        self.script = None
        self.name_prefix = None
        self.site = None
        self.count = 0
        self._set_fields(**kwargs)

    def _set_fields(self, **kwargs):
        """
        Universal setter for all fields.
        if you try to set a non-existent field.
        :param kwargs:
        :return: self to support call chaining
        """
        for k, v in kwargs.items():
            try:
                # will toss an exception if field is not defined
                self.__getattribute__(k)
                self.__setattr__(k, v)
            except AttributeError:
                raise Exception(f"Unable to set field {k} of capacity, no such field available "
                                f"{[k for k in self.__dict__.keys()]}")
        return self
