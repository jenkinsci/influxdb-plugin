static List<Map<String, String>> recommendedConfigurations() {
    def configurations = [
        [ platform: "linux", jdk: "17"],
        [ platform: "windows", jdk: "17"]
    ]
    return configurations
}
buildPlugin(useContainerAgent: true,
            configurations: recommendedConfigurations())
