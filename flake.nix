{
  description = "Development environment for Hodor";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forEachSystem = nixpkgs.lib.genAttrs supportedSystems;
    in
    {
      devShells = forEachSystem (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
        in
        {
          default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              # Runtime y kit de desarrollo para Java 25
              openjdk25

              # Gestor de dependencias y construccion
              maven

              # Orquestacion de contenedores locales
              docker-compose

              # Herramientas de linea de comandos para PostgreSQL 17 (psql, pg_isready)
              postgresql_17
            ];

            # Variables de entorno declaradas de forma nativa
            JAVA_HOME = "${pkgs.openjdk25}";

            shellHook = ''
              echo "Hodor development environment loaded successfully."
            '';
          };
        }
      );
    };
}
