import re
class ObjectType:
    def __init__(self, attr):
        regex = "class\(total:(\\d+)\)|structure\(total:(\\d+)\)"
        ret = re.findall(regex, attr)
        if not ret:
            return
        member_num = ret[0][0] if ret[0][0] != '' else ret[0][1]

        regex = "class\(total:\\d+\)|structure\(total:\\d+\)"
        ret = re.findall(regex, attr)

        self.type = ret[0].split("(")[0]
        self.member_num = int(member_num)
        self.members = {}

        members = attr[attr.find(ret[0]) + len(ret[0]) + 1:]
        for num in range(0, int(member_num)):
            name_with_postfix = members[members.find("|name:") + len("|name:"):]
            name = name_with_postfix[:name_with_postfix.find("|")]
            value_with_postfix = name_with_postfix[len(name) + 1 : name_with_postfix.rfind("}") + 1]
            if name == "":
                name = "object"
            if "class(" in value_with_postfix or "structure(" in value_with_postfix:
                value = ObjectType(value_with_postfix)
                self.members[name] = value
                if int(member_num) > 1:
                    members = value_with_postfix[value_with_postfix.find(str(value)) + len(str(value)) + 1:]
            else:
                split = value_with_postfix.find(":") + 1
                before = value_with_postfix.find(",")
                after = value_with_postfix.find("}")
                pos = before if before < after else after
                pos = pos if pos >= split else split
                value = value_with_postfix[split : pos]
                self.members[name] = value
                if int(member_num) > 1:
                    members = value_with_postfix[pos + 1:]

    def __str__(self):
        '''
        class(total:1){member 0|name:|Value:class(total:2){member 0|name:first|Value:structure(total:2){member 0|name:next|Value:,member 1|name:info|Value:},member 1|name:last|Value:}}  isNull:  False  TYPE:  class
        '''
        con =  self.type + "(total:" + str(self.member_num) + "){"
        i = 0
        for key, value in self.members.items():
            con += "member " + str(i) + "|name:" + (key if key != "object" else "") + "|Value:" + str(value) + ","
            i += 1
        con = con[:len(con) - 1] + "}"
        return con
