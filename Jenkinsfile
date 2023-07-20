static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.387.3"
    def configurations = [
        [ platform: "linux", jdk: "11", jenkins: recentLTS],
        [ platform: "windows", jdk: "11", jenkins: recentLTS]
    ]
    return configurations
}
buildPlugin(configurations: recommendedConfigurations())
