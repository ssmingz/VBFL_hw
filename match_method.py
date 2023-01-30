import os
import collections
import lizard
import sys

#available_bugs = [5,10,16]
#graph_map.txt格式:源文件,方法名&起始行&结束行,图文件
#for i in range(1, 43):
#for i in available_bugs:
i = sys.argv[1]
graph_root = f'/mnt/autoRun/graphs/{i}/graph'
output_file = f'/mnt/values/{i}/graph_map.txt'
method_dict=collections.OrderedDict()
if not os.path.exists(graph_root):
    print(f'[ERROR] no graph generated')
for graph_file in os.listdir(graph_root):
    lineNo = ''
    abspath = ''
    with open(f'{graph_root}/{graph_file}', 'r') as g:
        for line in g:
            if line.startswith('location##'):
                lineNo = line.split('##')[1].split('|')[0]
            if line.startswith('absFilename##'):
                abspath = line.split('##')[1].strip()
                if lineNo != '0':
                    break
    methods = lizard.analyze_file(abspath)
    flag = False
    for method in methods.function_list:
        if method.start_line == method.end_line:
            continue
        if method.start_line <= int(lineNo) <= method.end_line:
            actual_name = f'{abspath},{method.unqualified_name}&{method.start_line}&{method.end_line}'
            if method.unqualified_name != '_GLIBCXX_VISIBILITY':
                method_dict[actual_name] = f'{graph_root}/{graph_file}'
                flag = True
            break
    if not flag:
        print(f'[ERROR] not find method for graph file: {graph_root}/{graph_file}')
with open(output_file, 'w') as f:
    for m_new, m_src in method_dict.items():
        f.write(f'{m_new},{m_src}\n')
print(f'{i} ok')
print('finish')