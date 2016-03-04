# encoding: utf-8
import sys
import math


class Point:
    def __init__(self, _x, _y):
        self.x = _x
        self.y = _y

    def left(self):
        if not hasattr(self, "_left"):
            self._left = Point(self.x, self.y+1)
        return self._left

    def right(self):
        if not hasattr(self, "_right"):
            self._right = Point(self.x, self.y-1)
        return self._right

    def up(self):
        if not hasattr(self, "_up"):
            self._up = Point(self.x+1, self.y)
        return self._up

    def down(self):
        if not hasattr(self, "_down"):
            self._down = Point(self.x-1, self.y)
        return self._down


class Character:
    def __init__(self, _id, _x, _y):
        self.id = _id
        self.point = Point(_x, _y)

    def x(self):
        return self.point.x

    def y(self):
        return self.point.y


class Item:
    def __init__(self, _x, _y):
        self.point = Point(_x, _y)

    def x(self):
        return self.point.x

    def y(self):
        return self.point.y


class Map:
    class Cell:
        def __init__(self, _x, _y, _state):
            self.point = Point(_x, _y)
            self.state = _state

        def has_object(self):
            return self.state == "W" or self.state == "O"

        def x(self):
            return self.point.x

        def y(self):
            return self.point.y

    def __init__(self):
        _size = input().split(" ")
        self.row = int(_size[0])
        self.col = int(_size[1])
        self.cells = [[0 for i in range(self.col)] for j in range(self.row)]

        for _x in range(self.row):
            _line = list(input())
            for _y, _state in enumerate(_line):
                self.cells[_x][_y] = self.Cell(_x, _y, _state)

        # 忍者
        self.ninjas = list()
        for i in range(int(input())):
            _chars = input().split(" ")
            self.ninjas.append(Character(int(_chars[0]), int(_chars[1]), int(_chars[2])))

        # 敵
        self.enemies = list()
        for i in range(int(input())):
            _chars = input().split(" ")
            self.enemies.append(Character(int(_chars[0]), int(_chars[1]), int(_chars[2])))

        # アイテム
        self.items = list()
        for i in range(int(input())):
            _item = input().split(" ")
            self.items.append(Item(int(_item[0]), int(_item[1])))

        self.skll_count = list(input())

    def nearest_item(self, _x, _y):
        _distances = [math.sqrt(abs(_item.x() - _x)**2 + abs(_item.y() - _y)**2) for _item in self.items]
        _idx = _distances.index(min(_distances))
        return self.items[_idx]

    def movable(self, _point):
        return not self.cells[_point.x][_point.y].has_object()

    def xy2idx(self, _x, _y):
        return _x * self.col + _y

    def idx2xy(self, _idx):
        return (math.floor(_idx / self.col), _idx % self.col)

    def shortest_road(self, _start_point, _end_point):
        # 初期化
        _map_scores = [None for i in range(self.col*self.row)]

        for _idx, _score in enumerate(_map_scores):
            _x, _y = self.idx2xy(_idx)
            if self.cells[_x][_y].has_object():
                _map_scores[_idx] = -1

        # 最短経路の算出
        i = 0
        _map_scores[self.xy2idx(_start_point.x(), _start_point.y())] = 0
        while None in _map_scores:
            _changed = False
            for _idx, _score in enumerate(_map_scores):
                if _score == i:
                    _x, _y = self.idx2xy(_idx)
                    _point = self.cells[_x][_y].point
                    for _po in [_point.left(), _point.right(), _point.up(), _point.down()]:
                        if self.movable(_po) and _map_scores[self.xy2idx(_po.x, _po.y)] is None:
                            _map_scores[self.xy2idx(_po.x, _po.y)] = i+1
                            _changed = True
            if not _changed: break
            i += 1

        _step = _map_scores[self.xy2idx(_end_point.x(), _end_point.y())]
        _steps = []
        if _step is None:
            return _steps
        _next_po = self.cells[_end_point.x()][_end_point.y()].point
        for _score in list(reversed(range(_step))):
            for _po, _code in [(_next_po.left(), "L"), (_next_po.right(), "R"), (_next_po.up(), "U"), (_next_po.down(), "D")]:
                if _map_scores[self.xy2idx(_po.x, _po.y)] == _score:
                    _steps.append(_code)
                    _next_po = _po
                    break
        return list(reversed(_steps))


class Player:
    def __init__(self):
        self.point = input()
        self.map = Map()


class Skill:
    def __init__(self, _id, _cost):
        self.id = _id
        self.cost = _cost


class AI:
    def __init__(self, _name):
        self.name = _name

    def think(self):
        print(self.name)
        sys.stdout.flush()

        while 1:
            # 制限時間
            self.timelimit = input()

            # スキルの定義
            _skill_num = input()
            self.skills = []
            for _id, _cost in enumerate(input().split(" ")):
                self.skills.append(Skill(_id, _cost))

            # 順番に注意(インプットを順に呼んでるため)
            self.me = Player()
            self.rival = Player()

            # スキルは使わないので2固定
            print("2")

            for _ninja in self.me.map.ninjas:
                _item = self.me.map.nearest_item(_ninja.x(), _ninja.y())
                _steps = self.me.map.shortest_road(_ninja, _item)
                print("".join(_steps[0:2]))

            sys.stdout.flush()

# 起動
ai = AI("SampleAI.py")
ai.think()
