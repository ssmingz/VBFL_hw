import collections
import os

class Var2Line:
    def __init__(self, method_name, tree_file, value_file):
        self.method_id = method_name.split(':')[0]
        self.src_file = method_name.split(':')[1].split('#')[0]
        self.method_name = method_name.split(':')[1].split('#')[2]
        self.tree_file = tree_file
        self.value_file = value_file

    def direct_var_to_line(self):
        line_list = collections.OrderedDict()
        beginning = False
        with open(self.tree_file, 'r') as f:
            for l in f:
                if beginning:
                    lineNo = l.split(' ')[1].split('-')[1].split('/')[0]
                    if lineNo.isdigit():
                        inst = {}
                        inst['reorder_score'] = l.split(' ')[2]
                        inst['pdg_score'] = l.split(' ')[3]
                        inst['ori_variable'] = l.split(' ')[1]
                        line_list[lineNo] = inst
                if l.strip() == 'Reorder:':
                    beginning = True
        return line_list

    def find_inst_in_values(self, attr_name):
        lineNo, colNo = attr_name.split('-')[-1].split('/')[0], attr_name.split('-')[-1].split('/')[1]
        loc = f':{lineNo}|{colNo}'
        with open(self.value_file, 'r') as f:
            for l in f:
                if loc in l:
                    for e in l.split():
                        if loc in e:
                            return e
        return ''

    def map_args(self, line_mapping):
        line_mapping_without_args = collections.OrderedDict()
        for lineNo, info in line_mapping.items():
            inst = self.find_inst_in_values(info['ori_variable'])
            if inst.startswith('MethodArgument#'):
                # 用候选列表中第一次使用参数行替换原始行
                arg_name = inst.split('-')[-1]
                new_lineNo2 = 99999999999
                for lineNo2 in line_mapping.keys():
                    if lineNo2 != lineNo:
                        src_code = os.popen(f'sed -n {lineNo2}p {self.src_file}').read()
                        if arg_name in src_code:
                            if int(lineNo2) < new_lineNo2 and int(lineNo2) > int(lineNo):
                                new_lineNo2 = int(lineNo2)
                if str(new_lineNo2) not in line_mapping_without_args:
                    line_mapping_without_args[str(new_lineNo2)] = info
            else:
                if lineNo not in line_mapping_without_args:
                    line_mapping_without_args[lineNo] = info
        return line_mapping_without_args

    def print(self, line_mapping, output_path):
        with open(output_path, 'w') as f:
            for lineNo,info in line_mapping.items():
                f.write(f'{lineNo}:{info}\n')
