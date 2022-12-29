class SingleTest:
    def __init__(self, test_name, test_result, value_list):
        self.test_name = test_name
        self.test_result = test_result
        self.value_list = value_list

    def __str__(self):
        return self.test_name + "\n" + str(self.value_list) + "\n" + self.test_result

