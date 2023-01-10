import os
import sys

import single_test
import object_type
import re
import lizard

type_list = ["Condition", "MethodArgument", "Variable", "Binary_Expression", "Return_Expression"]
attr_type_list = ["Value", "isNull", "TYPE", "length"]  # see comments in get_func()
attr_postfix = "{PRED}"
method_map = {}
method_name_map = {}


def deal_with_Value(attr: str) -> list:
    """ [poi]
        only deal with '  Value:  '
        special treatment only for 'class' and 'structure'
        NOTE! HERE DOES NOT CONTAIN OTHER TYPES SUCH AS ARRAY
    """
    res_lst = []  # some tuple in list, (post_key, value)
    if "class(" in attr or "structure(" in attr:
        # regex = "class\(total:\\d+\)\{member \\d+\|name:[\\w\\d_]*\|Value:.*?" \
        #         "|structure\(total:\\d+\)\{member \\d+\|name:[\\w\\d_]*\|Value:.*?"

        obj = object_type.ObjectType(attr)
        # for each memeber in obj, set PRED to res_lst
        for name, member in obj.members.items():
            if isinstance(member, object_type.ObjectType):
                continue
            if member == "":
                continue
            name = attr_postfix + (name if name != "object" else "")
            value = member
            res_lst.append((name, value))
    else:
        pos = attr.find("/") if attr.find("/") != -1 else len(attr)
        value = attr[:pos].replace("Value:", "").strip()
        if value != "":
            res_lst.append(("", value))
    return res_lst


def deal_with_Attr(attr: str) -> list:
    """ [poi]
        only deal with attribute 'isNull'/'TYPE'/'length'
    """
    res_lst = []
    flag = [type for type in attr_type_list if type in attr]
    if not flag or len(flag) != 1:
        return res_lst
    pos = attr.find("/") if attr.find("/") != -1 else len(attr)
    value = attr[:pos].replace(flag[0] + ":", "").strip()
    res_lst.append((attr_postfix + "." + flag[0], value))
    return res_lst


def get_func():
    """ [poi]
        In order to split different types of results, make sure when you add additional attr_type in ${attr_type_list}
        do 1) add additional 'deal_with_${func_name_postfix}' method
           2) add 'deal_with_${func_name_postfix}' in ${func}
    """
    func = [deal_with_Value, deal_with_Attr, deal_with_Attr, deal_with_Attr]
    func_dict = dict(zip(attr_type_list, func))
    return func_dict


diff_pattern_func = get_func()


def read_values(path):
    with open(path, "r") as f:
        whole_file = f.read()
    return whole_file


def split_str_with_relst(values: str, regex_list: list) -> list:
    regex = "|".join(regex_list)
    regex = "(" + regex + ")"  # save regex itself when split
    values_split = [x.replace("\n", "") for x in re.split(regex, values.replace("\\n", "")) if x]
    single_values = []
    prefix = ""
    # concatenate values with its type
    for value_split in values_split:
        if value_split in regex_list:
            prefix = value_split
        else:
            single_values.append(prefix + value_split)
    return single_values


def read_attributes(attrs: str) -> list:
    attrs_lst = split_str_with_relst(attrs, ["  " + x + ":  " for x in attr_type_list])
    res_lst = []
    for attr in attrs_lst:
        flag = [type for type in attr_type_list if attr.startswith("  " + type + ":  ")]
        if not flag or len(flag) != 1:
            continue
        res_lst.extend(diff_pattern_func.get(flag[0])(attr))
    return res_lst


def parse_test(whole_file):
    method_index = 0
    result = []
    tests = whole_file.split("[ RUN")
    for t in tests[1:]:
        test_name = t.split("\n")[0].replace("]", "").strip()
        test_result = t.split(test_name)[-2].split("\n")[-1].strip()
        test_result = test_result.split("[")[1].replace("]", "").strip()
        test_result = test_result.replace("OK", "PASS").replace("FAILED", "FAIL")
        values = t.split(test_name)[1].split(test_result)[0].split("[")[0].strip()
        value_list = []

        # [poi]
        # 'vs = values.split("\n")' has an error:
        #     some attributes could occupy more than one line, split with '\n' might lead to attributes lost.
        # instead : use type_list to get single values
        vs = split_str_with_relst(values, type_list)

        for v in vs:
            v = v.strip()
            if v == "":
                continue

            if '/' not in v:
                continue
            var_type = v.split("/")[0]
            type_is_right = [ty for ty in type_list if ty in var_type]
            if not type_is_right:
                continue

            var_key = split_str_with_relst(v, ["  Value:"])
            if len(var_key) != 2:
                continue

            var_str = var_key[0].split(var_type)[1].split(":")[-1].strip()
            var_info = var_key[0].split(var_type)[1][:var_key[0].split(var_type)[1].find(var_str)-1].strip()
            
            # get real method name
            var_mname = var_info.split('|')[-1]
            src_file = var_info.split('|')[0]
            var_line = var_str.split("|")[0]
            var_col = var_str.split("|")[1].split("-")[0]
            addNew = True
            if method_name_map.get(var_mname) != None:
                for line_range, name in method_name_map.get(var_mname).items():
                    if int(line_range.split('_')[0]) <= int(var_line) <= int(line_range.split('_')[1]):
                        var_info = var_info.replace(f'|{var_mname}', f'|{name}')
                        addNew = False
                        break
            if addNew:
                methods = lizard.analyze_file(src_file)
                for m in methods.function_list:
                    if m.start_line <= int(var_line) <= m.end_line:
                        real_mname = f'{m.unqualified_name}&{m.start_line}&{m.end_line}'
                        line_range = f'{m.start_line}_{m.end_line}'
                        if var_mname in method_name_map.keys():
                            method_name_map[var_mname][line_range] = real_mname
                        else:
                            method_name_map[var_mname] = {}
                            method_name_map[var_mname][line_range] = real_mname
                        var_info = var_info.replace(f'|{var_mname}', f'|{real_mname}')
                        break
                        
            print(v)
            if "-" in var_str:
                var_name = var_str.split("-")[1]
            else:
                var_name = "EXP"
            # value = v.split(var_type)[1].split(" Value:")[1]
            # if value == "":
            #     continue
            # value = value.strip()
            # if "class" in value or "structure" in value:
            #     continue
            # value_list.append(var_name + "-" + var_line + "/" + var_col + ":" + value)

            # [poi] deal with different values
            values = var_key[1]
            attrs = read_attributes(values)
            # concatenate attrs with its method id, name, line|colum and value
            if method_map.get(var_info) is None:
                file_name = var_info.split('|')[0]
                method_name = var_info.split('|')[1]
                flag = True
                for full_method in method_map.keys():
                    # handle method name not valid output
                    if full_method.split('|')[1] in method_name and method_name.startswith('_') and full_method.split('|')[0] == file_name:
                        var_info = full_method
                        flag = False
                if flag:
                    method_map[var_info] = str(method_index)
                    method_index += 1
            for attr in attrs:
                single_value = method_map.get(var_info) + "#" + var_name + attr[0] + "-" + var_line + "/" + var_col + ":" + attr[1]
                value_list.append(single_value)
        result.append(single_test.SingleTest(test_name, test_result, value_list))
    return result


def main():
    whole_file = read_values(values_path)
    r = parse_test(whole_file)
    with open(output_path, "w") as total:
        for r1 in r:
            total.write(r1.test_name + "\n")
            for v1 in r1.value_list:
                total.write(v1 + "\n")
            total.write(r1.test_result + "\n")
    with open(instr_output_path, "w") as instr:
        for key, value in method_map.items():
            # todo need method type and arguments
            type = "?"
            args = "?"
            instr.write(value + ":" + key.split("|")[0] + "#" + type + "#" + key.split("|")[1] + "#" + args + "\n")
            each_output_path = output_dir + value + "/std_slicing.log"
            if not os.path.exists(output_dir + value):
                os.mkdir(output_dir + value)
            with open(each_output_path, "w") as each:
                for r1 in r:
                    each.write(r1.test_name + "\n")
                    for v1 in r1.value_list:
                        if v1.startswith(f'{value}#'):
                            each.write(v1.split("#")[1] + "\n")
                    each.write(r1.test_result + "\n")


def mainLS(values_pathL, values_pathS):
    r = []
    if os.path.exists(values_pathL):
        whole_fileL = read_values(values_pathL)
        r.extend(parse_test(whole_fileL))
    if os.path.exists(values_pathS):
        whole_fileS = read_values(values_pathS)
        r.extend(parse_test(whole_fileS))
    with open(output_path, "w") as total:
        for r1 in r:
            total.write(r1.test_name + "\n")
            for v1 in r1.value_list:
                total.write(v1 + "\n")
            total.write(r1.test_result + "\n")
    with open(instr_output_path, "w") as instr:
        for key, value in method_map.items():
            # todo need method type and arguments
            type = "?"
            args = "?"
            instr.write(value + ":" + key.split("|")[0] + "#" + type + "#" + key.split("|")[1] + "#" + args + "\n")
            each_output_path = output_dir + value + "/std_slicing.log"
            if not os.path.exists(output_dir + value):
                os.mkdir(output_dir + value)
            with open(each_output_path, "w") as each:
                for r1 in r:
                    each.write(r1.test_name + "\n")
                    for v1 in r1.value_list:
                        if v1.startswith(f'{value}#'):
                            each.write(v1.split("#")[1] + "\n")
                    each.write(r1.test_result + "\n")



if __name__ == '__main__':
    #root_dir = sys.argv[1]
    #output_dir = sys.argv[2]
    available_bugs = [2,3,4,5,6,7,8,9,10,12,14,16,17,20,22,23,24,25,26,28,29,30,31,35,36,37,38,39,40,41,42,44,45,46,48,49,50,51,52,53,58,59,60,61,62]
    available_bugs = [45]
    #for bugid in range(1,21):
    for bugid in available_bugs:
        root_dir = f'/mnt/values/{bugid}/'
        values_path = root_dir + "values.txt"
        values_pathL = root_dir + "values-large.txt"
        values_pathS = root_dir + "values-small.txt"
        if not os.path.exists(root_dir) or not os.path.exists(values_path) or not os.path.exists(values_pathL) or not os.path.exists(values_pathS):
            continue
        output_dir = f'/mnt/values/trees/bug_{bugid}/'
        if os.path.exists(output_dir):
            os.system(f'rm -rf {output_dir}*')
        else:
            os.makedirs(output_dir)
        output_path = output_dir + "original.txt"
        instr_output_path = output_dir + "instrumented_method_id.txt"
        if os.path.exists(values_pathL) or os.path.exists(values_pathS):
            mainLS(values_pathL, values_pathS)
        else:
            main()
        method_map.clear()
        method_name_map.clear()
    print("Finish")
