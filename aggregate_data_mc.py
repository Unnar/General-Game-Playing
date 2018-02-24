from collections import defaultdict
gameRole = defaultdict(list)
game = defaultdict(list)
try:
    while True:
        match = input().split('.')[1]
        role, roleid = input().split()[1:]
        steps = int(input().split()[1])
        score = int(input().split()[int(roleid)+1])
        avgsims = float(input().split()[1])
        nodes = int(input().split()[1])
        vals = gameRole[(match, role)]
        vals2 = game[match]
        if not vals2:
            game[match] = [steps, score, avgsims, nodes, 1]
        else:
            n = vals2[-1]
            game[match] = [(vals2[0]*n+steps)/(n+1), (vals2[1]*n+score)/(n+1), (vals2[2]*n+avgsims*steps)/(n+1), (vals2[3]*n+nodes)/(n+1), n+1] 
        if not vals:
            gameRole[(match, role)] = [steps, score, avgsims, nodes, 1]
        else:
            n = vals[-1]
            gameRole[(match, role)] = [(vals[0]*n+steps)/(n+1), (vals[1]*n+score)/(n+1), (vals[2]*n+avgsims*steps)/(n+1), (vals[3]*n+nodes)/(n+1), n+1]
except EOFError:
    pass
print("Game & Role & Average Steps & Average Score & Average simulations & Average Size of Tree & Number of times played\\\\\\hline")
for (match, role), vals in sorted(gameRole.items()):
    print("%s & %s & %.2f & %.2f & %.2f & %.2f & %d\\\\\\hline" %(match, role, vals[0], vals[1], vals[2], vals[3], vals[4]))

print("\n\n\n\n")    
print("Game & Average Steps & Average Score & Average simulations & Average Size of Tree & Number of times played\\\\\\hline")
for match, vals in sorted(game.items()):
    print("%s & %.2f & %.2f & %.2f & %.2f & %d\\\\\\hline" % (match, vals[0], vals[1], vals[2], vals[3], vals[4]))
