static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.289.1"
    def configurations = [
        [ platform: "linux", jdk: "8", jenkins: null ],
        [ platform: "windows", jdk: "8", jenkins: null ],
        [ platform: "linux", jdk: "8", jenkins: recentLTS ],
        [ platform: "windows", jdk: "8", jenkins: recentLTS ],
        [ platform: "linux", jdk: "11", jenkins: recentLTS ],
        [ platform: "windows", jdk: "11", jenkins: recentLTS ]
    ]
    return configurations
}
buildPlugin(configurations: recommendedConfigurations())
