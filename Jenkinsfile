static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.387.3"
    def configurations = [
        [ platform: "linux", jdk: "17", jenkins: recentLTS],
        [ platform: "windows", jdk: "17", jenkins: recentLTS]
    ]
    return configurations
}
buildPlugin(configurations: recommendedConfigurations())
