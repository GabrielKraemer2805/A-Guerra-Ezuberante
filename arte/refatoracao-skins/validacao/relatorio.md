# Validacao das skins refatoradas

## Refatoradas

- **marina_silva**: APROVADA (70 cores)
- **tabata_amaral**: APROVADA (77 cores)
- **haddad**: APROVADA (75 cores)
- **jones_manoel**: APROVADA (78 cores)
- **dilma**: APROVADA (80 cores)

## Originais (linha de base)

- **marina_silva**: REPROVADA (18 cores)
  - tronco/costas: base com buraco (32, 20, 40, 32)
  - camada externa cobre os olhos (6 de 6 pixels da linha dos olhos)
  - camada externa cobre o centro do rosto (24 pixels)
  - braco_d/direita: mao sem distincao da manga
  - braco_d/frente: mao sem distincao da manga
  - braco_d/esquerda: mao sem distincao da manga
  - braco_d/costas: mao sem distincao da manga
  - braco_e/direita: mao sem distincao da manga
  - braco_e/frente: mao sem distincao da manga
  - braco_e/esquerda: mao sem distincao da manga
  - braco_e/costas: mao sem distincao da manga
- **tabata_amaral**: REPROVADA (16 cores)
  - tronco/topo: base com buraco (20, 16, 28, 20)
  - tronco/base: base com buraco (28, 16, 36, 20)
  - perna_d/topo: base com buraco (4, 16, 8, 20)
  - perna_d/direita: base com buraco (0, 20, 4, 32)
  - perna_d/frente: base com buraco (4, 20, 8, 32)
  - perna_d/esquerda: base com buraco (8, 20, 12, 32)
  - perna_d/costas: base com buraco (12, 20, 16, 32)
  - perna_e/topo: base com buraco (20, 48, 24, 52)
  - perna_e/base: base com buraco (24, 48, 28, 52)
  - perna_e/direita: base com buraco (16, 52, 20, 64)
  - perna_e/frente: base com buraco (20, 52, 24, 64)
  - perna_e/esquerda: base com buraco (24, 52, 28, 64)
  - perna_e/costas: base com buraco (28, 52, 32, 64)
  - 32 pixels fora de regioes usadas pelo modelo
  - branco do olho aparece em direita da cabeca
  - branco do olho aparece em esquerda da cabeca
  - rosto com menos de 8 tons (chapado)
  - braco_d/direita: mao sem distincao da manga
  - braco_d/esquerda: mao sem distincao da manga
  - braco_d/costas: mao sem distincao da manga
  - braco_e/direita: mao sem distincao da manga
  - braco_e/esquerda: mao sem distincao da manga
  - braco_e/costas: mao sem distincao da manga
- **haddad**: REPROVADA (18 cores)
  - braco_d/esquerda: base com buraco (48, 20, 52, 32)
  - braco_e/esquerda: base com buraco (40, 52, 44, 64)
  - braco_d/direita: mao sem distincao da manga
  - braco_d/frente: mao sem distincao da manga
  - braco_d/esquerda: mao sem distincao da manga
  - braco_d/costas: mao sem distincao da manga
  - braco_e/direita: mao sem distincao da manga
  - braco_e/frente: mao sem distincao da manga
  - braco_e/esquerda: mao sem distincao da manga
  - braco_e/costas: mao sem distincao da manga
- **jones_manoel**: REPROVADA (199 cores)
  - alfa parcial (antialiasing ou desfoque nas bordas)
  - camada externa cobre os olhos (4 de 6 pixels da linha dos olhos)
  - camada externa cobre o centro do rosto (14 pixels)
  - rosto com menos de 8 tons (chapado)
- **dilma**: REPROVADA (641 cores)
  - alfa parcial (antialiasing ou desfoque nas bordas)
  - 307 pixels fora de regioes usadas pelo modelo
  - 641 cores: provavel imagem reduzida

## Rostos distintos (diferenca media por canal, 0-255)

- marina_silva x tabata_amaral: 45.4
- marina_silva x haddad: 37.1
- marina_silva x jones_manoel: 39.6
- marina_silva x dilma: 47.9
- tabata_amaral x haddad: 45.3
- tabata_amaral x jones_manoel: 64.3
- tabata_amaral x dilma: 28.8
- haddad x jones_manoel: 68.5
- haddad x dilma: 36.9
- jones_manoel x dilma: 71.3

## Integracao: textura do jogo igual a skin refatorada

- marina_silva: textura igual; fonte em assets-entrada igual
- tabata_amaral: textura igual; fonte em assets-entrada igual
- haddad: textura igual; fonte em assets-entrada igual
- jones_manoel: textura igual; fonte em assets-entrada igual
- dilma: textura igual; fonte em assets-entrada igual
