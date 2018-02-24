#include <bits/stdc++.h>
using namespace std;
template <class T> int size(const T &x) { return x.size(); }
#define rep(i,a,b) for (__typeof(a) i=(a); i<(b); ++i)
#define iter(it,c) for (__typeof((c).begin()) \
  it = (c).begin(); it != (c).end(); ++it)
typedef pair<int, int> ii;
typedef vector<int> vi;
typedef vector<ii> vii;
typedef long long ll;
const int INF = ~(1<<31);

const double EPS = 1e-9;
const double pi = acos(-1);
typedef unsigned long long ull;
typedef vector<vi> vvi;
typedef vector<vii> vvii;
template <class T> T smod(T a, T b) {
  return (a % b + b) % b; }

int main()
{
    ios_base::sync_with_stdio(false);
    
    map<string, double> scores;
    map<string, double> simulations;
    map<string, double> steps;
    map<string, double> nodes;
    map<string, int> counts;

    string cmd, line;
    while(getline(cin, line))
    {
        assert(cmd == "match");
        string matchid;
        cin >> matchid;
        int stop1 = matchid.find('.');
        assert(stop1 != string::npos);
        stop1++;
        int stop2 = matchid.find('.', stop1);
        assert(stop2 != string::npos);
        string game = matchid.substr(stop1, stop2-stop1);

        string role, rolename;
        int roleid;
        cin >> cmd;
        assert(cmd == "role");
        cin >> rolename >> roleid;
        double steps;
        cin >> cmd;
        assert(cmd == "steps");
        cin >> steps;
        
    }
    
    return 0;
}
