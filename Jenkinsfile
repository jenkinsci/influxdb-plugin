static List<Map<String, String>> recommendedConfigurations() {
    def recentLTS = "2.387.3"
    def configurations = [
        [ platform: "linux", jdk: "11", jenkins: recentLTS, javaLevel: "8" ],
        [ platform: "windows", jdk: "11", jenkins: recentLTS, javaLevel: "8" ]
    ]
    return configurations
}
buildPlugin(configurations: recommendedConfigurations())
