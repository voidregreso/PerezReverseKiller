package com.perez.revkiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Tree<T> {
    private static int dep;
    private static Stack<String> path;
    List<Map<String, String>> node;
    Comparator<String> sortByType = (a, b) -> {
        if(isDirectory(a) && !isDirectory(b))
            return -1;
        if(!isDirectory(a) && isDirectory(b))
            return 1;
        return a.toLowerCase().compareTo(b.toLowerCase());
    };

    public Tree(Set<String> names, HashMap<String, T> classMap) {
        if(path == null) {
            path = new Stack<>();
            dep = 0;
        }
        node = new ArrayList<>();
        for(String name : names) {
            String[] token = name.split("/");
            String tmp = "";
            for(int i = 0, len = token.length; i < len; i++) {
                String value = token[i];
                if(i >= node.size()) {
                    Map<String, String> map = new HashMap<>();
                    if(classMap.containsKey(tmp + value) && i + 1 == len)
                        map.put(tmp + value, tmp);
                    else
                        map.put(tmp + value + "/", tmp);
                    node.add(map);
                    tmp += value + "/";
                } else {
                    Map<String, String> map = node.get(i);
                    if(classMap.containsKey(tmp + value) && i + 1 == len)
                        map.put(tmp + value, tmp);
                    else
                        map.put(tmp + value + "/", tmp);
                    tmp += value + "/";
                }
            }
        }
    }

    private List<String> list(String parent) {
        Map<String, String> map = null;
        List<String> str = new ArrayList<>();
        while(dep >= 0 && node.size() > 0) {
            map = node.get(dep);
            if(map != null)
                break;
            pop();
        }
        if(map == null)
            return str;
        for(String key : map.keySet()) {
            if(parent.equals(map.get(key))) {
                int index;
                if(key.endsWith("/"))
                    index = key.lastIndexOf("/", key.length() - 2);
                else
                    index = key.lastIndexOf("/");
                if(index != -1)
                    key = key.substring(index + 1);
                str.add(key);

            }
        }
        Collections.sort(str, sortByType);
        return str;
    }

    public void addNode(String name) {
        Map<String, String> map = node.get(dep);
        map.put(getCurPath() + name, getCurPath());
    }

    public void deleteNode(String name) {
        Map<String, String> map = node.get(dep);
        map.remove(getCurPath() + name);
    }

    public List<String> list() {
        return list(getCurPath());
    }

    public void push(String name) {
        dep++;
        path.push(name);
    }

    public String pop() {
        if(dep > 0) {
            dep--;
            return path.pop();
        }
        return null;
    }

    public String getCurPath() {
        return join(path);
    }

    public boolean isDirectory(String name) {
        return name.endsWith("/");
    }

    private String join(Stack<String> stack) {
        StringBuilder sb = new StringBuilder();
        for(String s : stack)
            sb.append(s);
        return sb.toString();
    }

}