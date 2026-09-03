# -*- coding: utf-8 -*-
"""更新 meta.json：4 门方言 A1 追加课时 + 新增 A2/B1/B2 共 12 门课程。"""
import json, io, os

P = r"D:\Agent\WorkBuddy\Lingoplay\backend\src\main\resources\seed\meta.json"

# 每门方言：标题前缀 / cover / 新增课时 / 新课程
DIALECTS = {
    "zh-yue": {
        "name": "广东话", "cover": "🍵",
        "a1_units": [
            ("Unit 1 · 日常问候", "粤语问候与地道寒暄", [("词汇 · 人称与寒暄", "zh-yue_a1_u1_word2.json")]),
            ("Unit 2 · 屋企与数字", "家人称谓、数字与茶楼日常", [("词汇 · 屋企与数字", "zh-yue_a1_u2_word3.json")]),
        ],
        "desc": {
            "A2": "茶楼点单、逛街购物，学用地道粤语逛吃香港。",
            "B1": "长句对话与粤语俗语，听懂港片台词里的机锋。",
            "B2": "粤语成语歇后语进阶，讲嘢有晒味道。",
        },
        "new_units": {
            "A2": [
                ("Unit 1 · 茶楼与街市", "埋单打包、行街讲价", [("词汇 · 茶楼食玩", "zh-yue_a2_u1_word1.json")]),
                ("Unit 2 · 出行购物", "搭车问路、试衫刷卡", [("词汇 · 出行购物", "zh-yue_a2_u2_word2.json")]),
            ],
            "B1": [
                ("Unit 1 · 街坊对话", "劝人金句与处事长句", [("句式 · 街坊对话", "zh-yue_b1_u1_sentence1.json")]),
                ("Unit 2 · 俗语惯用", "港式俗语与生活智慧", [("句式 · 俗语惯用", "zh-yue_b1_u2_sentence2.json")]),
            ],
            "B2": [
                ("Unit 1 · 成语歇后语", "鹩哥豁口、水瓜打狗——讲得出就赢", [("句式 · 成语歇后语", "zh-yue_b2_u1_idiom1.json")]),
            ],
        },
        "levelName": {"A1": "入门", "A2": "初级", "B1": "中级", "B2": "进阶"},
    },
    "zh-sc": {
        "name": "四川话", "cover": "🌶️",
        "a1_units": [
            ("Unit 1 · 摆龙门阵", "四川话问候与寒暄", [("词汇 · 人称与寒暄", "zh-sc_a1_u1_word2.json")]),
            ("Unit 2 · 屋头与数字", "家人称谓、数字与赶场日常", [("词汇 · 屋头日常", "zh-sc_a1_u2_word3.json")]),
        ],
        "desc": {
            "A2": "烫火锅、吃串串，用四川话把安逸生活吃到巴适。",
            "B1": "川式长句与俗语，茶馆头摆龙门阵不怯场。",
            "B2": "川味歇后语进阶，说的就是那一口麻辣味。",
        },
        "new_units": {
            "A2": [
                ("Unit 1 · 麻辣吃玩", "烫火锅、打牙祭、好吃狗集合", [("词汇 · 麻辣吃玩", "zh-sc_a2_u1_word1.json")]),
                ("Unit 2 · 出行购物", "赶公交、倒拐问路、砍价", [("词汇 · 出行购物", "zh-sc_a2_u2_word2.json")]),
            ],
            "B1": [
                ("Unit 1 · 坝坝茶话", "劝人金句与处事长句", [("句式 · 坝坝茶话", "zh-sc_b1_u1_sentence1.json")]),
                ("Unit 2 · 俗语惯用", "雄起、假打、捡耙和", [("句式 · 俗语惯用", "zh-sc_b1_u2_sentence2.json")]),
            ],
            "B2": [
                ("Unit 1 · 歇后语进阶", "猫抓糍粑——脱不到爪爪", [("句式 · 歇后语进阶", "zh-sc_b2_u1_idiom1.json")]),
            ],
        },
        "levelName": {"A1": "入门", "A2": "初级", "B1": "中级", "B2": "进阶"},
    },
    "zh-bj": {
        "name": "北京话", "cover": "🏯",
        "a1_units": [
            ("Unit 1 · 胡同问候", "京腔京韵的称呼与寒暄", [("词汇 · 人称与寒暄", "zh-bj_a1_u1_word2.json")]),
            ("Unit 2 · 大院日常", "老街坊、遛弯儿与数字", [("词汇 · 大院日常", "zh-bj_a1_u2_word3.json")]),
        ],
        "desc": {
            "A2": "豆汁儿焦圈儿涮羊肉，把北京吃出味道来。",
            "B1": "京味长句与俗语，茶馆侃大山压得住场。",
            "B2": "北京歇后语进阶，言语儿里透着局气。",
        },
        "new_units": {
            "A2": [
                ("Unit 1 · 京味吃玩", "豆汁儿、听戏、贴秋膘", [("词汇 · 京味吃玩", "zh-bj_a2_u1_word1.json")]),
                ("Unit 2 · 出行购物", "挤公交、逛庙会、砍价", [("词汇 · 出行购物", "zh-bj_a2_u2_word2.json")]),
            ],
            "B1": [
                ("Unit 1 · 茶馆对白", "京味劝人金句与处事长句", [("句式 · 茶馆对白", "zh-bj_b1_u1_sentence1.json")]),
                ("Unit 2 · 俗语惯用", "猫儿腻、掉链子、侃大山", [("句式 · 俗语惯用", "zh-bj_b1_u2_sentence2.json")]),
            ],
            "B2": [
                ("Unit 1 · 歇后语进阶", "外甥打灯笼——照旧", [("句式 · 歇后语进阶", "zh-bj_b2_u1_idiom1.json")]),
            ],
        },
        "levelName": {"A1": "入门", "A2": "初级", "B1": "中级", "B2": "进阶"},
    },
    "zh-sh": {
        "name": "上海话", "cover": "🌃",
        "a1_units": [
            ("Unit 1 · 弄堂问候", "阿拉、伊——沪语开口第一步", [("词汇 · 人称与寒暄", "zh-sh_a1_u1_word2.json")]),
            ("Unit 2 · 屋里厢", "家人称谓、数字与弄堂生活", [("词汇 · 屋里厢", "zh-sh_a1_u2_word3.json")]),
        ],
        "desc": {
            "A2": "生煎小笼排骨年糕，用上海话吃遍梧桐区。",
            "B1": "沪语长句与俗语，谈山海经拎得清。",
            "B2": "沪上歇后语进阶，讲闲话老有腔调。",
        },
        "new_units": {
            "A2": [
                ("Unit 1 · 沪上吃玩", "生煎小笼、白相大世界", [("词汇 · 沪上吃玩", "zh-sh_a2_u1_word1.json")]),
                ("Unit 2 · 出行购物", "乘地铁、逛马路、掼派头", [("词汇 · 出行购物", "zh-sh_a2_u2_word2.json")]),
            ],
            "B1": [
                ("Unit 1 · 客堂间对话", "沪语处事金句与长句", [("句式 · 客堂间对话", "zh-sh_b1_u1_sentence1.json")]),
                ("Unit 2 · 俗语惯用", "捣浆糊、拎勿清、扎劲", [("句式 · 俗语惯用", "zh-sh_b1_u2_sentence2.json")]),
            ],
            "B2": [
                ("Unit 1 · 俗谚进阶", "螺丝壳里做道场", [("句式 · 俗谚进阶", "zh-sh_b2_u1_idiom1.json")]),
            ],
        },
        "levelName": {"A1": "入门", "A2": "初级", "B1": "中级", "B2": "进阶"},
    },
}

with io.open(P, encoding='utf-8') as f:
    meta = json.load(f)

courses = meta['courses']
by_key = {(c['language'], c['title']): c for c in courses}

def lesson(title, f, order):
    return {"title": title, "type": "WORD", "sortOrder": order, "contentFile": f}

added_courses = 0
added_lessons = 0

for code, cfg in DIALECTS.items():
    # ---- 1) A1 课程追加课时 ----
    a1 = by_key[(code, f"{cfg['name']} A1 · 入门")]
    existing_unit_titles = {u['title'] for u in a1['units']}
    # u1 追加 word2
    a1['units'][0]['lessons'].append(lesson(cfg['a1_units'][0][2][0][0], cfg['a1_units'][0][2][0][1], 2))
    added_lessons += 1
    # u2 新增
    ut, ud, u2_lessons = cfg['a1_units'][1]
    if ut not in existing_unit_titles:
        a1['units'].append({
            "title": ut, "description": ud, "sortOrder": 2,
            "lessons": [lesson(t, f2, k) for k, (t, f2) in enumerate(u2_lessons, start=1)],
        })
        added_lessons += len(u2_lessons)

    # ---- 2) 新增 A2/B1/B2 课程 ----
    for i, (lv, order) in enumerate([("A2", 2), ("B1", 3), ("B2", 4)]):
        title = f"{cfg['name']} {lv} · {cfg['levelName'][lv]}"
        if (code, title) in by_key:
            continue
        units = []
        for j, (ut, ud, lessons) in enumerate(cfg['new_units'][lv], start=1):
            units.append({
                "title": ut, "description": ud, "sortOrder": j,
                "lessons": [lesson(t, f2, k) for k, (t, f2) in enumerate(lessons, start=1)],
            })
            added_lessons += len(lessons)
        c = {
            "language": code, "title": title, "level": lv, "levelName": cfg['levelName'][lv],
            "description": cfg['desc'][lv], "cover": cfg['cover'], "sortOrder": order,
            "units": units,
        }
        courses.append(c)
        by_key[(code, title)] = c
        added_courses += 1

# 稳定排序：保持原顺序，新课程追加尾部
with io.open(P, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(meta, f, ensure_ascii=False, indent=2)
    f.write('\n')

print(f"added courses: {added_courses}, added lessons: {added_lessons}, total courses: {len(courses)}")
