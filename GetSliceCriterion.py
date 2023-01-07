# 运行此文件前应该已经运行过对应测试了 注意build_dest_dir需要和build.py里的dest_dir对应 需要参数 bug对应的id
import os
import sys
import bug
import lizard
import gcovMethod

# bug_number = sys.argv[1]
build_dest_dir = "/mnt/out_put"
bugs_txt_dir = "./bugs.txt"
bugs = []


def read_bugs_from_txt(file_path):
    with open(file_path, "r") as f:
        for line in f:
            bugs.append(bug.Bug(line))


def get_gcov(bug_id, build_path, old_gcov):
    if not bug_id.isdigit():
        print("Error bug id")
        return
    bid = int(bug_id)
    if bid <= 0:
        print("Error bug id")
        return
    current_bug = bugs[bid - 1]
    source_name: str = current_bug.bug_src
    gcda_name = source_name.split("/")[-1] + ".gcda"
    find_cmd = f"find {build_path}/build -name {gcda_name}"
    print(find_cmd)
    all_gcda = os.popen(find_cmd).read()
    all_gcdas = all_gcda.split("\n")
    os.chdir(f"{build_path}/build")
    for g in all_gcdas:
        if g == "":
            continue
        gcov_cmd = f"llvm-cov-12 gcov {g}"
        print(gcov_cmd)
        os.system(gcov_cmd)
        if old_gcov == 1:
            gcov_dir = f"{build_path}/gcov_old"
        else:
            gcov_dir = f"{build_path}/gcov"
        if not os.path.exists(gcov_dir):
            mkdir_gcov_cmd = f"mkdir {gcov_dir}"
            print(mkdir_gcov_cmd)
            os.system(mkdir_gcov_cmd)
        find_gcov_cmd = f"find {build_path}/build -name *'{source_name.split('/')[-1]}.gcov'"
        print(find_gcov_cmd)
        all_gcov = os.popen(find_gcov_cmd).read()
        all_gcovs = all_gcov.split("\n")
        for gcov in all_gcovs:
            if gcov == "":
                continue
            cp_gcov_cmd = f"cp {gcov} {gcov_dir}/{g.replace('/', '1').replace('.gcda','.gcov')}"
            #print(cp_gcov_cmd)
            os.system(cp_gcov_cmd)


def get_pass_gcov(bug_id, build_path, testname):
    if not bug_id.isdigit():
        print("Error bug id")
        return
    bid = int(bug_id)
    if bid <= 0:
        print("Error bug id")
        return
    current_bug = bugs[bid - 1]
    source_name: str = current_bug.bug_src
    gcda_name = source_name.split("/")[-1] + ".gcda"
    find_cmd = f"find {build_path}/build -name {gcda_name}"
    print(find_cmd)
    all_gcda = os.popen(find_cmd).read()
    all_gcdas = all_gcda.split("\n")
    os.chdir(f"{build_path}/build")
    for g in all_gcdas:
        if g == "":
            continue
        gcov_cmd = f"llvm-cov-12 gcov {g}"
        print(gcov_cmd)
        os.system(gcov_cmd)
        gcov_dir = f"{build_path}/pass_gcov/{testname}"
        if not os.path.exists(gcov_dir):
            os.makedirs(gcov_dir)
        find_gcov_cmd = f"find {build_path}/build -name *'{source_name.split('/')[-1]}.gcov'"
        print(find_gcov_cmd)
        all_gcov = os.popen(find_gcov_cmd).read()
        all_gcovs = all_gcov.split("\n")
        for gcov in all_gcovs:
            if gcov == "":
                continue
            cp_gcov_cmd = f"cp {gcov} {gcov_dir}/{g.replace('/', '1').replace('.gcda','.gcov')}"
            #print(cp_gcov_cmd)
            os.system(cp_gcov_cmd)



def get_all_gcov(bug_id):
    if not bug_id.isdigit():
        print("Error bug id")
        return
    bid = int(bug_id)
    if bid <= 0:
        print("Error bug id")
        return
    get_gcov(bug_id, build_dest_dir + f"/{bug_id}")


def clean_all_gcov(gcov_path):
    os.system(f"find {gcov_path}/build -name '*.gcda' | xargs rm -rf")
    find_gcov_cmd = f"find {gcov_path}/build -name '*.gcov' | xargs rm -rf"
    all_gcov = os.popen(find_gcov_cmd).read()
    all_gcovs = all_gcov.split("\n")
    for gcov in all_gcovs:
        if gcov == "":
            continue
        rm_gcov_cmd = f"rm {gcov}"
        #print(rm_gcov_cmd)
        #os.system(rm_gcov_cmd)


def parse_gcov(gcov_path):
    result = []
    gcov_files = os.listdir(gcov_path)
    for gcov_f in gcov_files:
        with open(f"{gcov_path}/{gcov_f}", "r") as gcov_file:
            gcov_methods = []
            i = 0
            last_cov_line = []
            start_compute = False
            for line in gcov_file:
                ls = line.split(":")
                line_no = ls[1]
                
                if line_no.strip() == "0" and ls[2] == "Source":
                    src_path = ls[3].replace("\n", "")
                    methods = lizard.analyze_file(f"{src_path}")
                    if len(methods.function_list) == 0:
                        break
                    for m in methods.function_list:
                        start_line = m.start_line
                        end_line = m.end_line
                        name = m.long_name.replace(' [ [ maybe_unused ] ]', '')
                        gcov_methods.append(gcovMethod.gcovMethod(start_line, end_line, name, 0, src_path, 0, 0))
                if i >= len(gcov_methods):
                    break
                current_method = gcov_methods[i]
                if int(line_no) > current_method.end_line:
                    i += 1
                    start_compute = False
                    current_method.cov_lines = last_cov_line
                    last_cov_line = []
                if int(line_no) >= current_method.start_line:
                    start_compute = True
                if start_compute:
                    last_cov_line.append(line)
                if int(line_no) == current_method.start_line:
                    current_method.cov_time = ls[0].strip().replace("*", "").replace("#", "").replace("-", "")
                    if current_method.cov_time != "":
                        print(current_method.name)
                        print(line)
                    if current_method.cov_time == "":
                        current_method.cov_time = 0
                    else:
                        current_method.cov_time = int(current_method.cov_time)
            if start_compute:
                current_method.cov_lines = last_cov_line
            result += gcov_methods
    return result


def compute_score(pass_methods, fail_methods):
    scores = []
    for p_method in pass_methods:
        for f_method in fail_methods:
            if p_method.name == f_method.name and p_method.src == f_method.src:
                #if f_method.cov_time == p_method.cov_time:
                #    score = -1
                #elif p_method.cov_time == 0:
                #    score = 1
                if f_method.cov_time == 0:
                    score = 0
                else:
                    #score = (f_method.cov_time - p_method.cov_time) / pow((f_method.cov_time - p_method.cov_time) * p_method.cov_time, 0.5)
                    score = f_method.cov_time / pow(f_method.cov_time*(f_method.cov_time + p_method.cov_time),0.5)
                p_method.score = score
                f_method.score = score
                scores.append(f_method)
    #for f_method in fail_methods:
    #    if f_method.score == 0:
    #        f_method.score = 1
    #        scores.append(f_method)
    scores.sort(key=lambda x: x.score, reverse=True)
    return scores


def check_slice_line(code_line):
    code_line = code_line.replace("\n", "").strip()
    if code_line == "}":
        return False
    elif "return" in code_line:
        return_str = code_line.split("return")[1].split(";")[0]
        if return_str.isdigit() or return_str == "":
            return False
    return True


def get_sbfl_rank(scores):
    ranks = []
    for score in scores[:]:
        last_cov_line = "-1"
        #print(score.cov_lines)
        #print(score.name)
        for line in score.cov_lines[::-1]:
            ls = line.split(":")
            line_no: str = ls[1]
            cover_time = ls[0].strip().replace("*", "").replace("#", "").replace("-", "")
            if not cover_time.isdigit():
                continue
            if not check_slice_line(ls[2]):
                continue
            last_cov_line = line_no
            print(line)
            break
        if last_cov_line == "-1":
            for line in score.cov_lines[::-1]:
                ls = line.split(":")
                line_no: str = ls[1]
                cover_time = ls[0].strip().replace("*", "").replace("#", "").replace("-", "")
                if not check_slice_line(ls[2]):
                    continue
                last_cov_line = line_no
                print(line)
                break
        mname = score.name.split('(')[0]
        if len(score.src.split("mysql-server-source")) >= 2:
            ranks.append(f'{score.src.split("mysql-server-source")[1]}|{mname}|{score.start_line}|{score.score}')
        else:
            ranks.append(f'{score.src.split("mysql-server-source")[0]}|{mname}|{score.start_line}|{score.score}')
        # print(score.name)
    return ranks


def get_slice_criterion(scores):
    slices = []
    for score in scores[:10]:
        last_cov_line = "-1"
        #print(score.cov_lines)
        #print(score.name)
        for line in score.cov_lines[::-1]:
            ls = line.split(":")
            line_no: str = ls[1]
            cover_time = ls[0].strip().replace("*", "").replace("#", "").replace("-", "")
            if not cover_time.isdigit():
                continue
            if not check_slice_line(ls[2]):
                continue
            last_cov_line = line_no
            print(line)
            break
        if last_cov_line == "-1":
            for line in score.cov_lines[::-1]:
                ls = line.split(":")
                line_no: str = ls[1]
                cover_time = ls[0].strip().replace("*", "").replace("#", "").replace("-", "")
                if not check_slice_line(ls[2]):
                    continue
                last_cov_line = line_no
                print(line)
                break
        if len(score.src.split("mysql-server-source")) >= 2:
            slices.append(score.src.split("mysql-server-source")[1] + "|" + score.name + "|" + last_cov_line)
        else:
            slices.append(score.src.split("mysql-server-source")[0] + "|" + score.name + "|" + last_cov_line)
        # print(score.name)
    return slices


def parse_gcov_fail(gcov_path):
    result = []
    gcov_files = os.listdir(gcov_path)
    for gcov_f in gcov_files:
        with open(f"{gcov_path}/{gcov_f}", "r") as gcov_file:
            src_path = ""
            ms = []
            for line in gcov_file:
                ls = line.split(":")
                line_no = ls[1]
                if line_no.strip() == "0" and ls[2] == "Source":
                    src_path = ls[3].replace("\n", "")
                    methods = lizard.analyze_file(f"{src_path}")
                    if len(methods.function_list) == 0:
                        break
                    for m in methods.function_list:
                        start_line = m.start_line
                        end_line = m.end_line
                        name = m.long_name.replace(' [ [ maybe_unused ] ]', '')
                        m_dict = {"start": start_line, "end": end_line, "name": name}
                        ms.append(m_dict)
                if line_no.strip() != "0":
                    m_name = ""
                    m_to_remove = []  # 因为line_no是递增的 用于移除已经过去了的方法
                    l = int(line_no.strip())
                    for m in ms:
                        if m["start"] <= l <= m["end"]:
                            m_name = m["name"]
                        elif m["end"] < l:
                            m_to_remove.append(m)
                    for m in m_to_remove:
                        ms.remove(m)
                    line_info = {"line_no": line_no.strip(), "src": src_path,
                                 "cov_time": ls[0].strip().replace("*", "").replace("#", "").replace("-", ""), "name":m_name}
                    result.append(line_info)
    return result



def get_fail_cover(pass_lines, fail_lines: list):
    covers = []
    for pline_info in pass_lines:
        temp = ""
        for fline_info in fail_lines:
            if pline_info["src"] == fline_info["src"] and pline_info["line_no"] == fline_info["line_no"]:
                temp = fline_info
                all_times = pline_info["cov_time"]
                fail_times = fline_info["cov_time"]
                if fail_times == "":
                    continue
                delta_times = 0
                if all_times == "":
                    delta_times = int(fail_times)
                else:
                    delta_times = int(fail_times) - int(all_times)
                if delta_times != 0:
                    if len(fline_info["src"].split("mysql-server-source")) >= 2:
                        covers.append(
                            fline_info["src"].split("mysql-server-source")[1] + "|" + fline_info["name"] + "|" + fline_info["line_no"])
                    else:
                        covers.append(
                            fline_info["src"].split("mysql-server-source")[0] + "|" + fline_info["name"] + "|" + fline_info["line_no"])
                break
        if temp != "":
            fail_lines.remove(temp)
    return covers
